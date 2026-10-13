package com.pintu.service.impl;

import com.pintu.common.exception.BusinessException;
import com.pintu.dto.CancelRequest;
import com.pintu.dto.PassengerOrderVO;
import com.pintu.dto.RecommendedTripVO;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;
import com.pintu.entity.Vehicle;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.MatchRequestMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.security.SensitiveDataCrypto;
import com.pintu.service.MapService;
import com.pintu.service.AddressService;
import com.pintu.service.SafetyService;
import com.pintu.service.matching.MatchEvaluation;
import com.pintu.service.matching.MatchingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PassengerOrderServiceImplTest {

    private PassengerOrderMapper passengerOrderMapper;
    private DriverTripMapper driverTripMapper;
    private OrderRecordMapper orderRecordMapper;
    private UserMapper userMapper;
    private VehicleMapper vehicleMapper;
    private MatchingEngine matchingEngine;
    private SafetyService safetyService;
    private PassengerOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        passengerOrderMapper = mock(PassengerOrderMapper.class);
        driverTripMapper = mock(DriverTripMapper.class);
        orderRecordMapper = mock(OrderRecordMapper.class);
        userMapper = mock(UserMapper.class);
        vehicleMapper = mock(VehicleMapper.class);
        matchingEngine = mock(MatchingEngine.class);
        safetyService = mock(SafetyService.class);
        service = new PassengerOrderServiceImpl(
                passengerOrderMapper,
                driverTripMapper,
                userMapper,
                vehicleMapper,
                orderRecordMapper,
                mock(MatchRequestMapper.class),
                new SensitiveDataCrypto("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="),
                mock(MapService.class),
                mock(AddressService.class),
                matchingEngine,
                safetyService
        );
    }

    @Test
    void cancelRestoresReservedSeatsAndReopensFullTrip() {
        PassengerOrder passengerOrder = passengerOrder("已确认");
        OrderRecord order = order("已确认");
        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        trip.setAvailableSeats(0);
        trip.setStatus("已满座");

        when(orderRecordMapper.selectById("order-1")).thenReturn(order);
        when(passengerOrderMapper.selectByIdForUpdate("po-1")).thenReturn(passengerOrder);
        when(orderRecordMapper.selectByIdForUpdate("order-1")).thenReturn(order);
        when(driverTripMapper.selectByIdForUpdate("trip-1")).thenReturn(trip);

        CancelRequest request = new CancelRequest();
        request.setReason("临时有事");
        PassengerOrderVO result = service.cancel("order-1", request, "passenger-001");

        assertEquals("已取消", order.getStatus());
        assertEquals("driver-001", result.getDriverId());
        assertEquals("已取消", passengerOrder.getStatus());
        assertEquals(2, trip.getAvailableSeats());
        assertEquals("已发布", trip.getStatus());
        verify(driverTripMapper).updateById(trip);
        verify(safetyService).closeOrderSafetyResources("order-1", "order_cancelled");
    }

    @Test
    void recommendRejectsAnotherPassengersOrder() {
        PassengerOrder passengerOrder = passengerOrder("待匹配");
        when(passengerOrderMapper.selectById("po-1")).thenReturn(passengerOrder);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.recommend("po-1", "passenger-002")
        );

        assertEquals(1004, error.getCode());
    }

    @Test
    void recommendMasksDriverNameAndVehiclePlate() {
        PassengerOrder passengerOrder = passengerOrder("待匹配");
        passengerOrder.setDepartureTime(LocalDateTime.now().plusHours(1));
        passengerOrder.setStartLng(new BigDecimal("121.5063770"));
        passengerOrder.setStartLat(new BigDecimal("31.2820800"));
        passengerOrder.setEndLng(new BigDecimal("121.4896200"));
        passengerOrder.setEndLat(new BigDecimal("31.2582100"));

        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        trip.setDriverId("driver-001");
        trip.setVehicleId("veh-1");
        trip.setStartLocation("同济科技园");
        trip.setEndLocation("SOHO");
        trip.setStartLng(new BigDecimal("121.5070000"));
        trip.setStartLat(new BigDecimal("31.2825000"));
        trip.setEndLng(new BigDecimal("121.4900000"));
        trip.setEndLat(new BigDecimal("31.2586000"));
        trip.setDepartureTime(LocalDateTime.now().plusHours(1));
        trip.setAvailableSeats(3);
        trip.setPrice(new BigDecimal("18.60"));

        User passenger = user("passenger-001", "王小明", 96);
        User driver = user("driver-001", "赵司机", 95);
        Vehicle vehicle = new Vehicle();
        vehicle.setId("veh-1");
        vehicle.setPlate("沪A12348");
        vehicle.setModel("秦PLUS");
        vehicle.setColor("白色");

        when(passengerOrderMapper.selectById("po-1")).thenReturn(passengerOrder);
        when(driverTripMapper.selectList(any())).thenReturn(List.of(trip));
        when(userMapper.selectById("passenger-001")).thenReturn(passenger);
        when(userMapper.selectById("driver-001")).thenReturn(driver);
        when(vehicleMapper.selectById("veh-1")).thenReturn(vehicle);
        when(matchingEngine.evaluate(passengerOrder, trip, passenger, driver))
                .thenReturn(new MatchEvaluation(true, 0.1, 0.2, 5, 90, 88, 92, "顺路推荐"));

        List<RecommendedTripVO> result = service.recommend("po-1", "passenger-001");

        assertEquals(1, result.size());
        assertEquals("赵**", result.get(0).getDriverName());
        assertEquals("沪A***8", result.get(0).getVehicle().getPlate());
        assertFalse(result.get(0).getDriverName().contains("司机"));
        assertFalse(result.get(0).getVehicle().getPlate().contains("1234"));
    }

    private PassengerOrder passengerOrder(String status) {
        PassengerOrder passengerOrder = new PassengerOrder();
        passengerOrder.setId("po-1");
        passengerOrder.setPassengerId("passenger-001");
        passengerOrder.setStartLocation("同济科技园");
        passengerOrder.setEndLocation("SOHO");
        passengerOrder.setPassengerCount(2);
        passengerOrder.setStatus(status);
        return passengerOrder;
    }

    private User user(String id, String name, int credit) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setCredit(credit);
        user.setStatus("正常");
        return user;
    }

    private OrderRecord order(String status) {
        OrderRecord order = new OrderRecord();
        order.setId("order-1");
        order.setPassengerOrderId("po-1");
        order.setDriverTripId("trip-1");
        order.setPassengerId("passenger-001");
        order.setDriverId("driver-001");
        order.setPassengerCount(2);
        order.setStatus(status);
        return order;
    }
}
