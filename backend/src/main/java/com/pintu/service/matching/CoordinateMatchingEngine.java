package com.pintu.service.matching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pintu.common.util.GeoDistance;
import com.pintu.entity.Blacklist;
import com.pintu.entity.DriverTrip;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;
import com.pintu.mapper.BlacklistMapper;
import com.pintu.service.RuleRuntimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CoordinateMatchingEngine implements MatchingEngine {
    private final BlacklistMapper blacklistMapper;
    private final RuleRuntimeService ruleRuntimeService;
    private final double radiusKm;
    private final long timeWindowMinutes;
    private final int minimumOverlap;
    private final int minimumCredit;
    private final double routeWeight;
    private final double timeWeight;
    private final double creditWeight;
    private final double priceWeight;

    public CoordinateMatchingEngine(
            BlacklistMapper blacklistMapper,
            RuleRuntimeService ruleRuntimeService,
            @Value("${matching.radius-km:3.0}") double radiusKm,
            @Value("${matching.time-window-minutes:45}") long timeWindowMinutes,
            @Value("${matching.minimum-overlap:60}") int minimumOverlap,
            @Value("${matching.minimum-credit:60}") int minimumCredit,
            @Value("${matching.weights.route:0.45}") double routeWeight,
            @Value("${matching.weights.time:0.25}") double timeWeight,
            @Value("${matching.weights.credit:0.20}") double creditWeight,
            @Value("${matching.weights.price:0.10}") double priceWeight) {
        this.blacklistMapper = blacklistMapper;
        this.ruleRuntimeService = ruleRuntimeService;
        this.radiusKm = radiusKm;
        this.timeWindowMinutes = timeWindowMinutes;
        this.minimumOverlap = minimumOverlap;
        this.minimumCredit = minimumCredit;
        this.routeWeight = routeWeight;
        this.timeWeight = timeWeight;
        this.creditWeight = creditWeight;
        this.priceWeight = priceWeight;
    }

    @Override
    public MatchEvaluation evaluate(PassengerOrder order, DriverTrip trip, User passenger, User driver) {
        double startDistance = GeoDistance.km(order.getStartLng(), order.getStartLat(),
                trip.getStartLng(), trip.getStartLat());
        double endDistance = GeoDistance.km(order.getEndLng(), order.getEndLat(),
                trip.getEndLng(), trip.getEndLat());
        double effectiveRadiusKm = ruleRuntimeService.doubleValue("MATCHING", "DEFAULT", "radiusKm", radiusKm);
        long effectiveTimeWindowMinutes = ruleRuntimeService.longValue(
                "MATCHING", "DEFAULT", "timeWindowMinutes", timeWindowMinutes);
        int effectiveMinimumOverlap = ruleRuntimeService.intValue(
                "MATCHING", "DEFAULT", "minimumOverlap", minimumOverlap);
        int effectiveMinimumCredit = ruleRuntimeService.intValue(
                "MATCHING", "DEFAULT", "minimumCredit", minimumCredit);
        double effectiveRouteWeight = ruleRuntimeService.doubleValue(
                "MATCHING", "DEFAULT", "routeWeight", routeWeight);
        double effectiveTimeWeight = ruleRuntimeService.doubleValue(
                "MATCHING", "DEFAULT", "timeWeight", timeWeight);
        double effectiveCreditWeight = ruleRuntimeService.doubleValue(
                "MATCHING", "DEFAULT", "creditWeight", creditWeight);
        double effectivePriceWeight = ruleRuntimeService.doubleValue(
                "MATCHING", "DEFAULT", "priceWeight", priceWeight);
        long timeDifference = order.getDepartureTime() == null || trip.getDepartureTime() == null
                ? Long.MAX_VALUE
                : Math.abs(Duration.between(order.getDepartureTime(), trip.getDepartureTime()).toMinutes());
        int routeOverlap = clamp((int) Math.round(100
                - (startDistance + endDistance) / Math.max(0.1, effectiveRadiusKm * 2) * 40));
        int timeScore = clamp((int) Math.round(100
                - (double) timeDifference / Math.max(1, effectiveTimeWindowMinutes) * 100));
        int driverCredit = driver == null || driver.getCredit() == null ? 0 : driver.getCredit();
        int priceScore = trip.getPrice() == null ? 70
                : clamp((int) Math.round(100 - Math.max(0, trip.getPrice().doubleValue() - 10) * 2));
        int comprehensive = clamp((int) Math.round(
                routeOverlap * effectiveRouteWeight
                        + timeScore * effectiveTimeWeight
                        + Math.min(100, driverCredit) * effectiveCreditWeight
                        + priceScore * effectivePriceWeight));

        String rejection = hardFilterReason(order, trip, passenger, driver, startDistance,
                endDistance, timeDifference, routeOverlap, effectiveRadiusKm,
                effectiveTimeWindowMinutes, effectiveMinimumOverlap, effectiveMinimumCredit);
        String reason = rejection == null
                ? String.format("起点相距 %.1fkm，终点相距 %.1fkm，时间相差 %d 分钟，顺路度 %d%%",
                startDistance, endDistance, timeDifference, routeOverlap)
                : rejection;
        return new MatchEvaluation(rejection == null, startDistance, endDistance,
                timeDifference, routeOverlap, timeScore, comprehensive, reason);
    }

    private String hardFilterReason(
            PassengerOrder order, DriverTrip trip, User passenger, User driver,
            double startDistance,
            double endDistance,
            long timeDifference,
            int overlap,
            double effectiveRadiusKm,
            long effectiveTimeWindowMinutes,
            int effectiveMinimumOverlap,
            int effectiveMinimumCredit) {
        if (trip.getAvailableSeats() == null || order.getPassengerCount() == null
                || trip.getAvailableSeats() < order.getPassengerCount()) return "剩余座位不足";
        if (driver == null || driver.getCredit() == null || driver.getCredit() < effectiveMinimumCredit) {
            return "司机信用分低于门槛";
        }
        if (passenger == null || passenger.getCredit() == null || passenger.getCredit() < effectiveMinimumCredit) {
            return "乘客信用分低于门槛";
        }
        if ("已封禁".equals(driver.getStatus()) || "限制接单".equals(driver.getStatus())) return "司机账号受限";
        if ("已封禁".equals(passenger.getStatus()) || "限制发单".equals(passenger.getStatus())) return "乘客账号受限";
        if (isBlocked(order.getPassengerId(), trip.getDriverId())) return "双方存在黑名单关系";
        if (startDistance > effectiveRadiusKm) return "起点距离超过匹配半径";
        if (endDistance > effectiveRadiusKm) return "终点距离超过匹配半径";
        if (timeDifference > effectiveTimeWindowMinutes) return "出发时间超出匹配窗口";
        if (overlap < effectiveMinimumOverlap) return "路线顺路度不足";
        return null;
    }

    private boolean isBlocked(String passengerId, String driverId) {
        if (passengerId == null || driverId == null) return false;
        return blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                .and(wrapper -> wrapper
                        .eq(Blacklist::getOwnerId, passengerId).eq(Blacklist::getBlockedId, driverId)
                        .or()
                        .eq(Blacklist::getOwnerId, driverId).eq(Blacklist::getBlockedId, passengerId))) > 0;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
