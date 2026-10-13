package com.pintu.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pintu.common.exception.BusinessException;
import com.pintu.dto.EmergencyContactSaveRequest;
import com.pintu.dto.EmergencyContactVO;
import com.pintu.dto.SafetyAlertCreateRequest;
import com.pintu.dto.SafetyAlertVO;
import com.pintu.entity.EmergencyContact;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.SafetyAlert;
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
import com.pintu.service.police.PoliceReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SafetyServiceImplTest {

    private EmergencyContactMapper emergencyContactMapper;
    private SafetyAlertMapper safetyAlertMapper;
    private OrderRecordMapper orderRecordMapper;
    private PoliceGateway policeGateway;
    private SafetyServiceImpl service;

    @BeforeEach
    void setUp() {
        emergencyContactMapper = mock(EmergencyContactMapper.class);
        safetyAlertMapper = mock(SafetyAlertMapper.class);
        orderRecordMapper = mock(OrderRecordMapper.class);
        policeGateway = mock(PoliceGateway.class);
        byte[] key = new byte[32];
        SensitiveDataCrypto crypto = new SensitiveDataCrypto(Base64.getEncoder().encodeToString(key));
        service = new SafetyServiceImpl(
                emergencyContactMapper,
                mock(DataRetentionPolicyMapper.class),
                safetyAlertMapper,
                orderRecordMapper,
                mock(VirtualPhoneBindingMapper.class),
                mock(TripShareTokenMapper.class),
                mock(LocationReportMapper.class),
                mock(TrajectoryPointMapper.class),
                mock(UserMapper.class),
                crypto,
                policeGateway,
                new MockVirtualPhoneProvider(),
                mock(SecurityAuditService.class)
        );
    }

    @Test
    void createsContactWithEncryptedPhoneAndMaskedResponse() {
        when(emergencyContactMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        EmergencyContactVO result = service.createEmergencyContact("passenger-001", contactRequest(true));

        ArgumentCaptor<EmergencyContact> captor = ArgumentCaptor.forClass(EmergencyContact.class);
        verify(emergencyContactMapper).insert(captor.capture());
        EmergencyContact stored = captor.getValue();
        assertEquals("passenger-001", stored.getUserId());
        assertNotEquals("13812345678", stored.getPhoneCipher());
        assertTrue(stored.getPhoneCipher().startsWith("gcm:"));
        assertEquals("138****5678", stored.getPhoneMasked());
        assertEquals("138****5678", result.getPhoneMasked());
        assertFalse(result.toString().contains("13812345678"));
    }

    @Test
    void contactListQueryIsScopedToCurrentUser() {
        when(emergencyContactMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(contact("contact-1", "passenger-001", 0)));

        List<EmergencyContactVO> result = service.listEmergencyContacts("passenger-001");

        ArgumentCaptor<QueryWrapper<EmergencyContact>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(emergencyContactMapper).selectList(captor.capture());
        assertEquals(1, result.size());
        assertTrue(captor.getValue().getSqlSegment().contains("user_id"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("passenger-001"));
    }

    @Test
    void settingPrimaryContactClearsPreviousPrimaryForSameUser() {
        EmergencyContact previous = contact("contact-old", "passenger-001", 1);
        when(emergencyContactMapper.selectList(any(Wrapper.class))).thenReturn(List.of(previous));

        service.createEmergencyContact("passenger-001", contactRequest(true));

        assertEquals(0, previous.getIsPrimary());
        verify(emergencyContactMapper).updateById(previous);
    }

    @Test
    void updateAndDeleteRejectContactsOutsideCurrentUser() {
        when(emergencyContactMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> service.updateEmergencyContact("passenger-001", "contact-other", contactRequest(false))
        );
        assertThrows(
                BusinessException.class,
                () -> service.deleteEmergencyContact("passenger-001", "contact-other")
        );
        verify(emergencyContactMapper, never()).deleteById(any(String.class));
    }

    @Test
    void alertRejectsNonParticipantAndDoesNotPersist() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.ONGOING.getValue()));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.createOrderAlert("other-user", "order-1", alertRequest())
        );

        assertEquals(1004, error.getCode());
        verify(safetyAlertMapper, never()).insert(any(SafetyAlert.class));
    }

    @Test
    void alertRejectsOrderThatIsNotOngoing() {
        when(orderRecordMapper.selectById("order-1")).thenReturn(order(OrderStatus.CONFIRMED.getValue()));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.createOrderAlert("passenger-001", "order-1", alertRequest())
        );

        assertEquals(1014, error.getCode());
    }

    @Test
    void participantAlertPersistsLocationReportsAndLeavesOrderStatusUnchanged() {
        OrderRecord order = order(OrderStatus.ONGOING.getValue());
        when(orderRecordMapper.selectById("order-1")).thenReturn(order);
        when(policeGateway.report(any(SafetyAlert.class)))
                .thenReturn(new PoliceReport("police-1", "mock reported"));

        SafetyAlertVO result = service.createOrderAlert("driver-001", "order-1", alertRequest());

        ArgumentCaptor<SafetyAlert> captor = ArgumentCaptor.forClass(SafetyAlert.class);
        verify(safetyAlertMapper).insert(captor.capture());
        verify(policeGateway).report(captor.getValue());
        verify(safetyAlertMapper).updateById(captor.getValue());
        assertEquals("driver-001", captor.getValue().getUserId());
        assertEquals(new BigDecimal("121.5000000"), captor.getValue().getLongitude());
        assertEquals("已上报", result.getStatus());
        assertEquals("police-1", result.getPoliceReportId());
        assertEquals(OrderStatus.ONGOING.getValue(), order.getStatus());
        verify(orderRecordMapper, never()).updateById(any(OrderRecord.class));
    }

    private EmergencyContactSaveRequest contactRequest(boolean primary) {
        EmergencyContactSaveRequest request = new EmergencyContactSaveRequest();
        request.setName("家人");
        request.setPhone("13812345678");
        request.setRelationship("家属");
        request.setIsPrimary(primary);
        return request;
    }

    private EmergencyContact contact(String id, String userId, int primary) {
        EmergencyContact contact = new EmergencyContact();
        contact.setId(id);
        contact.setUserId(userId);
        contact.setName("旧联系人");
        contact.setPhoneCipher("gcm:encrypted");
        contact.setPhoneMasked("139****0000");
        contact.setIsPrimary(primary);
        contact.setCreatedAt(LocalDateTime.of(2026, 6, 15, 8, 0));
        return contact;
    }

    private OrderRecord order(String status) {
        OrderRecord order = new OrderRecord();
        order.setId("order-1");
        order.setPassengerId("passenger-001");
        order.setDriverId("driver-001");
        order.setStatus(status);
        return order;
    }

    private SafetyAlertCreateRequest alertRequest() {
        SafetyAlertCreateRequest request = new SafetyAlertCreateRequest();
        request.setAlertType("一键报警");
        request.setLongitude(new BigDecimal("121.5000000"));
        request.setLatitude(new BigDecimal("31.2000000"));
        request.setDetails("课程演示");
        return request;
    }
}
