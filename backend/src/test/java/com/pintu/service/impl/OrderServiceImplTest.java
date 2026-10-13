package com.pintu.service.impl;

import com.pintu.common.exception.BusinessException;
import com.pintu.dto.AdminOrderVO;
import com.pintu.dto.AdminOrderQuery;
import com.pintu.dto.AdminOrderSummaryVO;
import com.pintu.dto.DriverOrderVO;
import com.pintu.dto.PaymentRequest;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.User;
import com.pintu.entity.Vehicle;
import com.pintu.enums.OrderStatus;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.PaymentMapper;
import com.pintu.mapper.ReviewMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.service.SafetyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private OrderRecordMapper orderRecordMapper;
    private DriverTripMapper driverTripMapper;
    private UserMapper userMapper;
    private VehicleMapper vehicleMapper;
    private SafetyService safetyService;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orderRecordMapper = mock(OrderRecordMapper.class);
        driverTripMapper = mock(DriverTripMapper.class);
        userMapper = mock(UserMapper.class);
        vehicleMapper = mock(VehicleMapper.class);
        safetyService = mock(SafetyService.class);
        service = new OrderServiceImpl(
                orderRecordMapper,
                mock(PassengerOrderMapper.class),
                mock(PaymentMapper.class),
                mock(ReviewMapper.class),
                userMapper,
                vehicleMapper,
                driverTripMapper,
                safetyService
        );
    }

    @Test
    void payRejectsOrderBeforeArrival() {
        OrderRecord order = order(OrderStatus.CONFIRMED.getValue(), "18.60");
        when(orderRecordMapper.selectByIdForUpdate("order-1")).thenReturn(order);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.pay("order-1", payment("18.60"), "passenger-001")
        );

        assertEquals(1014, error.getCode());
    }

    @Test
    void payRejectsMismatchedAmount() {
        OrderRecord order = order(OrderStatus.PENDING_PAYMENT.getValue(), "18.60");
        when(orderRecordMapper.selectByIdForUpdate("order-1")).thenReturn(order);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.pay("order-1", payment("1.00"), "passenger-001")
        );

        assertEquals(1014, error.getCode());
    }

    @Test
    void startTripRejectsAnotherDriver() {
        OrderRecord order = order(OrderStatus.CONFIRMED.getValue(), "18.60");
        when(orderRecordMapper.selectByIdForUpdate("order-1")).thenReturn(order);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.startTrip("order-1", "driver-002")
        );

        assertEquals(1004, error.getCode());
    }

    @Test
    void startTripStopsTheDriverTripFromReceivingNewMatches() {
        OrderRecord order = order(OrderStatus.CONFIRMED.getValue(), "18.60");
        order.setDriverTripId("trip-1");
        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        trip.setStatus("已发布");
        when(orderRecordMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(driverTripMapper.selectByIdForUpdate("trip-1")).thenReturn(trip);

        service.startTrip("order-1", "driver-001");

        assertEquals(OrderStatus.ONGOING.getValue(), order.getStatus());
        assertEquals(OrderStatus.ONGOING.getValue(), trip.getStatus());
        assertNotNull(order.getActualDeparture());
        verify(driverTripMapper).updateById(trip);
        verify(safetyService).ensureVirtualPhoneBound("order-1", "driver-001");
    }

    @Test
    void arriveRecordsActualArrivalTime() {
        OrderRecord order = order(OrderStatus.ONGOING.getValue(), "18.60");
        order.setDriverTripId("trip-1");
        order.setActualDeparture(LocalDateTime.now().minusHours(1));
        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        when(orderRecordMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(driverTripMapper.selectByIdForUpdate("trip-1")).thenReturn(trip);

        service.arrive("order-1", "driver-001");

        assertEquals(OrderStatus.PENDING_PAYMENT.getValue(), order.getStatus());
        assertNotNull(order.getActualArrival());
        verify(safetyService).closeOrderSafetyResources("order-1", "trip_arrived");
    }

    @Test
    void driverOrderListMasksPassengerNameAndVehiclePlate() {
        OrderRecord order = order(OrderStatus.CONFIRMED.getValue(), "18.60");
        order.setDriverTripId("trip-1");
        order.setStartLocation("同济科技园");
        order.setEndLocation("SOHO");
        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        trip.setVehicleId("veh-1");
        Vehicle vehicle = new Vehicle();
        vehicle.setId("veh-1");
        vehicle.setPlate("沪A12348");
        vehicle.setModel("秦PLUS");

        when(orderRecordMapper.selectList(any())).thenReturn(List.of(order));
        when(userMapper.selectById("passenger-001")).thenReturn(user("passenger-001", "王乘客"));
        when(driverTripMapper.selectById("trip-1")).thenReturn(trip);
        when(vehicleMapper.selectById("veh-1")).thenReturn(vehicle);

        List<DriverOrderVO> result = service.listByDriver("driver-001");

        assertEquals(1, result.size());
        assertEquals("王**", result.get(0).getPassengerName());
        assertEquals("沪A***8 · 秦PLUS", result.get(0).getVehicle());
        assertFalse(result.get(0).getPassengerName().contains("乘客"));
        assertFalse(result.get(0).getVehicle().contains("1234"));
    }

    @Test
    void adminOrderListMasksPassengerAndDriverNames() {
        OrderRecord order = order(OrderStatus.PENDING_PAYMENT.getValue(), "18.60");
        order.setStartLocation("同济科技园");
        order.setEndLocation("SOHO");

        when(orderRecordMapper.selectList(any())).thenReturn(List.of(order));
        when(userMapper.selectById("passenger-001")).thenReturn(user("passenger-001", "王乘客"));
        when(userMapper.selectById("driver-001")).thenReturn(user("driver-001", "赵司机"));

        List<AdminOrderVO> result = service.listForAdmin(null);

        assertEquals(1, result.size());
        assertEquals("王**", result.get(0).getPassenger());
        assertEquals("赵**", result.get(0).getDriver());
        assertFalse(result.get(0).getPassenger().contains("乘客"));
        assertFalse(result.get(0).getDriver().contains("司机"));
    }

    @Test
    void adminOrderSummaryCountsStatusesAndPayments() {
        OrderRecord pending = order(OrderStatus.PENDING_PAYMENT.getValue(), "18.60");
        pending.setPaymentStatus("未支付");
        OrderRecord completed = order(OrderStatus.COMPLETED.getValue(), "20.00");
        completed.setId("order-2");
        completed.setPaymentStatus("已支付");
        when(orderRecordMapper.selectList(any())).thenReturn(List.of(pending, completed));

        AdminOrderSummaryVO result = service.summarizeForAdmin(new AdminOrderQuery());

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPendingPayment());
        assertEquals(1, result.getCompleted());
        assertEquals(1, result.getStatusCounts().get(OrderStatus.PENDING_PAYMENT.getValue()));
        assertEquals(1, result.getPaymentStatusCounts().get("已支付"));
    }

    @Test
    void adminOrderListBuildsFilterQuery() {
        OrderRecord order = order(OrderStatus.COMPLETED.getValue(), "18.60");
        when(orderRecordMapper.selectList(any())).thenReturn(List.of(order));
        AdminOrderQuery query = new AdminOrderQuery();
        query.setStatus(OrderStatus.COMPLETED.getValue());
        query.setPaymentStatus("已支付");
        query.setKeyword("order-1");
        query.setDateFrom("2026-06-01");
        query.setDateTo("2026-06-30");

        service.listForAdmin(query);

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderRecord>> captor =
                forClass(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class);
        verify(orderRecordMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("payment_status"));
        assertTrue(sql.contains("created_at"));
        assertTrue(sql.contains("LIKE"));
    }

    private OrderRecord order(String status, String price) {
        OrderRecord order = new OrderRecord();
        order.setId("order-1");
        order.setPassengerId("passenger-001");
        order.setDriverId("driver-001");
        order.setStatus(status);
        order.setPaymentStatus("未支付");
        order.setPrice(new BigDecimal(price));
        return order;
    }

    private User user(String id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private PaymentRequest payment(String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal(amount));
        request.setMethod("mock_balance");
        return request;
    }
}
