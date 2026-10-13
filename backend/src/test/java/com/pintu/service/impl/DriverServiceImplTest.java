package com.pintu.service.impl;

import com.pintu.common.exception.BusinessException;
import com.pintu.dto.DriverTripCreateRequest;
import com.pintu.dto.RecommendedPassengerVO;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;
import com.pintu.entity.Vehicle;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.mapper.VehicleMapper;
import com.pintu.mapper.VehicleAttachmentMapper;
import com.pintu.mapper.AuditRecordMapper;
import com.pintu.service.MapService;
import com.pintu.service.AddressService;
import com.pintu.service.matching.MatchingEngine;
import com.pintu.service.matching.MatchEvaluation;
import com.pintu.mapper.PassengerOrderMapper;
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
import static org.mockito.Mockito.when;

class DriverServiceImplTest {

    private UserMapper userMapper;
    private VehicleMapper vehicleMapper;
    private DriverTripMapper driverTripMapper;
    private PassengerOrderMapper passengerOrderMapper;
    private MatchingEngine matchingEngine;
    private DriverServiceImpl service;

    @BeforeEach
    void setUp() {
        driverTripMapper = mock(DriverTripMapper.class);
        userMapper = mock(UserMapper.class);
        vehicleMapper = mock(VehicleMapper.class);
        passengerOrderMapper = mock(PassengerOrderMapper.class);
        matchingEngine = mock(MatchingEngine.class);
        service = new DriverServiceImpl(
                driverTripMapper,
                vehicleMapper,
                userMapper,
                mock(VehicleAttachmentMapper.class),
                mock(AuditRecordMapper.class),
                mock(MapService.class),
                mock(AddressService.class),
                passengerOrderMapper,
                matchingEngine
        );
    }

    @Test
    void createTripRejectsVehicleOwnedByAnotherDriver() {
        User driver = new User();
        driver.setId("driver-001");
        driver.setCredit(96);
        driver.setStatus("正常");

        Vehicle vehicle = new Vehicle();
        vehicle.setId("veh-002");
        vehicle.setDriverId("driver-002");
        vehicle.setStatus("已认证");
        vehicle.setSeats(4);

        when(userMapper.selectById("driver-001")).thenReturn(driver);
        when(vehicleMapper.selectById("veh-002")).thenReturn(vehicle);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.createTrip(request())
        );

        assertEquals(1004, error.getCode());
    }

    @Test
    void recommendPassengersMasksPassengerName() {
        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        trip.setDriverId("driver-001");
        trip.setStartLng(new BigDecimal("121.5070000"));
        trip.setStartLat(new BigDecimal("31.2825000"));
        trip.setEndLng(new BigDecimal("121.4900000"));
        trip.setEndLat(new BigDecimal("31.2586000"));
        trip.setDepartureTime(LocalDateTime.now().plusHours(1));
        trip.setAvailableSeats(3);

        PassengerOrder order = new PassengerOrder();
        order.setId("po-1");
        order.setPassengerId("passenger-001");
        order.setStartLocation("同济科技园");
        order.setEndLocation("SOHO");
        order.setStartLng(new BigDecimal("121.5063770"));
        order.setStartLat(new BigDecimal("31.2820800"));
        order.setEndLng(new BigDecimal("121.4896200"));
        order.setEndLat(new BigDecimal("31.2582100"));
        order.setDepartureTime(LocalDateTime.now().plusHours(1));
        order.setPassengerCount(1);

        User driver = user("driver-001", "赵司机", 96);
        User passenger = user("passenger-001", "王乘客", 95);

        when(driverTripMapper.selectById("trip-1")).thenReturn(trip);
        when(userMapper.selectById("driver-001")).thenReturn(driver);
        when(userMapper.selectById("passenger-001")).thenReturn(passenger);
        when(passengerOrderMapper.selectList(any())).thenReturn(List.of(order));
        when(matchingEngine.evaluate(order, trip, passenger, driver))
                .thenReturn(new MatchEvaluation(true, 0.1, 0.2, 5, 90, 88, 92, "顺路推荐"));

        List<RecommendedPassengerVO> result = service.recommendPassengers("driver-001", "trip-1");

        assertEquals(1, result.size());
        assertEquals("王**", result.get(0).getPassengerName());
        assertFalse(result.get(0).getPassengerName().contains("乘客"));
    }

    private DriverTripCreateRequest request() {
        DriverTripCreateRequest request = new DriverTripCreateRequest();
        request.setDriverId("driver-001");
        request.setVehicleId("veh-002");
        request.setStart("同济科技园");
        request.setEnd("SOHO");
        request.setDepartureTime(LocalDateTime.now().plusHours(1));
        request.setAvailableSeats(3);
        return request;
    }

    private User user(String id, String name, int credit) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setCredit(credit);
        user.setStatus("正常");
        return user;
    }
}
