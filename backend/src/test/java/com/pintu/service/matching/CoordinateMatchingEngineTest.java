package com.pintu.service.matching;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pintu.entity.Blacklist;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;
import com.pintu.mapper.BlacklistMapper;
import com.pintu.mapper.RuleConfigMapper;
import com.pintu.mapper.RuleVersionMapper;
import com.pintu.service.RuleRuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoordinateMatchingEngineTest {
    private BlacklistMapper blacklistMapper;
    private CoordinateMatchingEngine engine;

    @BeforeEach
    void setUp() {
        blacklistMapper = mock(BlacklistMapper.class);
        when(blacklistMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        RuleRuntimeService ruleRuntimeService = new RuleRuntimeService(
                mock(RuleConfigMapper.class),
                mock(RuleVersionMapper.class),
                new ObjectMapper());
        engine = new CoordinateMatchingEngine(
                blacklistMapper, ruleRuntimeService, 3.0, 45, 60, 60,
                0.45, 0.25, 0.20, 0.10);
    }

    @Test
    void acceptsNearbyRouteWithinTimeWindow() {
        MatchEvaluation result = engine.evaluate(order(1), trip(3, 10), user(95), user(96));
        assertTrue(result.eligible());
        assertTrue(result.routeOverlap() >= 60);
        assertTrue(result.comprehensiveScore() >= 60);
    }

    @Test
    void rejectsInsufficientSeats() {
        assertFalse(engine.evaluate(order(3), trip(2, 10), user(95), user(96)).eligible());
    }

    @Test
    void rejectsOutsideTimeWindow() {
        assertFalse(engine.evaluate(order(1), trip(3, 60), user(95), user(96)).eligible());
    }

    @Test
    void rejectsLowCredit() {
        assertFalse(engine.evaluate(order(1), trip(3, 10), user(95), user(40)).eligible());
    }

    @Test
    void rejectsBidirectionalBlacklist() {
        when(blacklistMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertFalse(engine.evaluate(order(1), trip(3, 10), user(95), user(96)).eligible());
    }

    private PassengerOrder order(int count) {
        PassengerOrder value = new PassengerOrder();
        value.setPassengerId("passenger-001");
        value.setPassengerCount(count);
        value.setStartLng(new BigDecimal("121.5063770"));
        value.setStartLat(new BigDecimal("31.2820800"));
        value.setEndLng(new BigDecimal("121.4896200"));
        value.setEndLat(new BigDecimal("31.2582100"));
        value.setDepartureTime(LocalDateTime.now().plusHours(1));
        return value;
    }

    private DriverTrip trip(int seats, int minuteOffset) {
        DriverTrip value = new DriverTrip();
        value.setDriverId("driver-001");
        value.setAvailableSeats(seats);
        value.setStartLng(new BigDecimal("121.5070000"));
        value.setStartLat(new BigDecimal("31.2825000"));
        value.setEndLng(new BigDecimal("121.4900000"));
        value.setEndLat(new BigDecimal("31.2586000"));
        value.setDepartureTime(LocalDateTime.now().plusHours(1).plusMinutes(minuteOffset));
        value.setPrice(new BigDecimal("18.60"));
        return value;
    }

    private User user(int credit) {
        User value = new User();
        value.setCredit(credit);
        value.setStatus("正常");
        return value;
    }
}
