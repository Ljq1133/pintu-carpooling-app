package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.dto.LocationBatchReportRequest;
import com.pintu.dto.LocationReportRequest;
import com.pintu.dto.TripShareViewVO;
import com.pintu.dto.VirtualPhoneBindingVO;
import com.pintu.entity.LocationReport;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.SafetyAlert;
import com.pintu.entity.TrajectoryPoint;
import com.pintu.entity.TripShareToken;
import com.pintu.entity.User;
import com.pintu.entity.VirtualPhoneBinding;
import com.pintu.enums.OrderStatus;
import com.pintu.mapper.EmergencyContactMapper;
import com.pintu.mapper.DataRetentionPolicyMapper;
import com.pintu.mapper.LocationReportMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.SafetyAlertMapper;
import com.pintu.mapper.TrajectoryPointMapper;
import com.pintu.mapper.TripShareTokenMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VirtualPhoneBindingMapper;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.SecurityAuditService;
import com.pintu.service.phone.MockVirtualPhoneProvider;
import com.pintu.service.police.PoliceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SafetyJklServiceImplTest {

    private SafetyAlertMapper safetyAlertMapper;
    private OrderRecordMapper orderRecordMapper;
    private VirtualPhoneBindingMapper virtualPhoneBindingMapper;
    private TripShareTokenMapper tripShareTokenMapper;
    private LocationReportMapper locationReportMapper;
    private TrajectoryPointMapper trajectoryPointMapper;
    private UserMapper userMapper;
    private SafetyServiceImpl service;

    @BeforeEach
    void setUp() {
        safetyAlertMapper = mock(SafetyAlertMapper.class);
        orderRecordMapper = mock(OrderRecordMapper.class);
        virtualPhoneBindingMapper = mock(VirtualPhoneBindingMapper.class);
        tripShareTokenMapper = mock(TripShareTokenMapper.class);
        locationReportMapper = mock(LocationReportMapper.class);
        trajectoryPointMapper = mock(TrajectoryPointMapper.class);
        userMapper = mock(UserMapper.class);
        byte[] key = new byte[32];
        service = new SafetyServiceImpl(
                mock(EmergencyContactMapper.class),
                mock(DataRetentionPolicyMapper.class),
                safetyAlertMapper,
                orderRecordMapper,
                virtualPhoneBindingMapper,
                tripShareTokenMapper,
                locationReportMapper,
                trajectoryPointMapper,
                userMapper,
                new SensitiveDataCrypto(Base64.getEncoder().encodeToString(key)),
                mock(PoliceGateway.class),
                new MockVirtualPhoneProvider(),
                mock(SecurityAuditService.class)
        );
    }

    @Test
    void participantCanBindVirtualPhoneForOngoingOrder() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.ONGOING.getValue()));
        when(virtualPhoneBindingMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        VirtualPhoneBindingVO result = service.ensureVirtualPhoneBound("order-1", "passenger-001");

        assertEquals("order-1", result.getOrderId());
        assertTrue(result.getVirtualNumber().startsWith("95013"));
        verify(virtualPhoneBindingMapper).insert(any(VirtualPhoneBinding.class));
    }

    @Test
    void shareTokenViewReturnsOnlyMaskedMinimalTripData() {
        TripShareToken token = new TripShareToken();
        token.setToken("token-1");
        token.setOrderId("order-1");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        when(tripShareTokenMapper.selectOne(any(Wrapper.class))).thenReturn(token);
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.ONGOING.getValue()));
        when(userMapper.selectById("passenger-001")).thenReturn(user("passenger-001", "Alice Passenger"));
        when(userMapper.selectById("driver-001")).thenReturn(user("driver-001", "Bob Driver"));

        TripShareViewVO result = service.getShareView("token-1");

        assertEquals("order-1", result.getOrderId());
        assertFalse(result.getRoute().contains("TongjiTechPark"));
        assertFalse(result.getPassenger().contains("Passenger"));
        assertEquals(new BigDecimal("121.507"), result.getStartLng());
    }

    @Test
    void terminalOrderRejectsPreciseLocationReport() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.COMPLETED.getValue()));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.reportDriverLocation("driver-001", "order-1", locationRequest(LocalDateTime.now()))
        );

        assertEquals(1014, error.getCode());
    }

    @Test
    void batchLocationReportStoresBatchAndBackoffFields() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.ONGOING.getValue()));
        when(locationReportMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(trajectoryPointMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        LocationReportRequest first = locationRequest(LocalDateTime.now());
        first.setClientReportId("client-1");
        first.setReportIntervalSeconds(3);
        first.setRetryCount(2);

        LocationBatchReportRequest batch = new LocationBatchReportRequest();
        batch.setBatchId("batch-1");
        batch.setPoints(List.of(first, locationRequest(LocalDateTime.now().plusSeconds(3))));

        service.reportDriverLocationBatch("driver-001", "order-1", batch);

        ArgumentCaptor<LocationReport> reports = ArgumentCaptor.forClass(LocationReport.class);
        verify(locationReportMapper, org.mockito.Mockito.times(2)).insert(reports.capture());
        assertEquals("batch-1", reports.getAllValues().get(0).getBatchId());
        assertEquals("client-1", reports.getAllValues().get(0).getClientReportId());
        assertEquals(3, reports.getAllValues().get(0).getReportIntervalSeconds());
        assertEquals(2, reports.getAllValues().get(0).getRetryCount());
    }

    @Test
    void staleLocationReportPersistsTrajectoryAndWarning() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.ONGOING.getValue()));
        when(locationReportMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(trajectoryPointMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.reportDriverLocation(
                "driver-001",
                "order-1",
                locationRequest(LocalDateTime.now().minusMinutes(5))
        );

        verify(locationReportMapper).insert(any(LocationReport.class));
        verify(trajectoryPointMapper).insert(any(TrajectoryPoint.class));
        ArgumentCaptor<SafetyAlert> alert = ArgumentCaptor.forClass(SafetyAlert.class);
        verify(safetyAlertMapper).insert(alert.capture());
        assertEquals("LOCATION_STALE", alert.getValue().getAlertType());
    }

    private LocationReportRequest locationRequest(LocalDateTime reportedAt) {
        LocationReportRequest request = new LocationReportRequest();
        request.setLongitude(new BigDecimal("121.5000000"));
        request.setLatitude(new BigDecimal("31.2000000"));
        request.setReportedAt(reportedAt);
        return request;
    }

    private OrderRecord order(String status) {
        OrderRecord order = new OrderRecord();
        order.setId("order-1");
        order.setPassengerId("passenger-001");
        order.setDriverId("driver-001");
        order.setStartLocation("TongjiTechPark");
        order.setEndLocation("SOHO");
        order.setStartLng(new BigDecimal("121.5065000"));
        order.setStartLat(new BigDecimal("31.2821000"));
        order.setEndLng(new BigDecimal("121.4503000"));
        order.setEndLat(new BigDecimal("31.2284000"));
        order.setDepartureTime(LocalDateTime.of(2026, 6, 16, 18, 30));
        order.setStatus(status);
        return order;
    }

    private User user(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
