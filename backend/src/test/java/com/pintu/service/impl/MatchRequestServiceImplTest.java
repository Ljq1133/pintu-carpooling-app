package com.pintu.service.impl;

import com.pintu.common.exception.BusinessException;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.MatchRequest;
import com.pintu.entity.OrderRecord;
import com.pintu.entity.PassengerOrder;
import com.pintu.mapper.DriverTripMapper;
import com.pintu.mapper.MatchRequestMapper;
import com.pintu.mapper.OrderRecordMapper;
import com.pintu.mapper.PassengerOrderMapper;
import com.pintu.mapper.UserMapper;
import com.pintu.service.matching.MatchingEngine;
import com.pintu.mapper.ConversationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchRequestServiceImplTest {

    private MatchRequestMapper matchRequestMapper;
    private PassengerOrderMapper passengerOrderMapper;
    private DriverTripMapper driverTripMapper;
    private OrderRecordMapper orderRecordMapper;
    private MatchRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        matchRequestMapper = mock(MatchRequestMapper.class);
        passengerOrderMapper = mock(PassengerOrderMapper.class);
        driverTripMapper = mock(DriverTripMapper.class);
        orderRecordMapper = mock(OrderRecordMapper.class);
        service = new MatchRequestServiceImpl(
                matchRequestMapper,
                passengerOrderMapper,
                driverTripMapper,
                orderRecordMapper,
                mock(UserMapper.class),
                mock(MatchingEngine.class),
                mock(ConversationMapper.class)
        );
    }

    @Test
    void acceptRejectsWhenRemainingSeatsAreInsufficient() {
        prepareLockedEntities(2, 1);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.accept("req-1", "driver-001")
        );

        assertEquals(1007, error.getCode());
    }

    @Test
    void acceptRejectsWhenPassengerOrderAlreadyHasFormalOrder() {
        prepareLockedEntities(1, 3);
        when(orderRecordMapper.selectLatestByPassengerOrderForUpdate("po-1")).thenReturn(new OrderRecord());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.accept("req-1", "driver-001")
        );

        assertEquals(1008, error.getCode());
    }

    @Test
    void getRejectsUnrelatedUser() {
        MatchRequest request = new MatchRequest();
        request.setId("req-1");
        request.setPassengerId("passenger-001");
        request.setDriverId("driver-001");
        when(matchRequestMapper.selectById("req-1")).thenReturn(request);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.get("req-1", "passenger-002")
        );

        assertEquals(1004, error.getCode());
    }

    private void prepareLockedEntities(int passengerCount, int availableSeats) {
        MatchRequest request = new MatchRequest();
        request.setId("req-1");
        request.setPassengerOrderId("po-1");
        request.setDriverTripId("trip-1");
        request.setDriverId("driver-001");
        request.setStatus("待处理");

        PassengerOrder passengerOrder = new PassengerOrder();
        passengerOrder.setId("po-1");
        passengerOrder.setPassengerCount(passengerCount);
        passengerOrder.setStatus("协商中");

        DriverTrip trip = new DriverTrip();
        trip.setId("trip-1");
        trip.setAvailableSeats(availableSeats);
        trip.setStatus("已发布");

        when(matchRequestMapper.selectByIdForUpdate("req-1")).thenReturn(request);
        when(passengerOrderMapper.selectByIdForUpdate("po-1")).thenReturn(passengerOrder);
        when(driverTripMapper.selectByIdForUpdate("trip-1")).thenReturn(trip);
    }
}
