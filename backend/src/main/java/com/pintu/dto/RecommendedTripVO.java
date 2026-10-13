package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐司机行程视图对象。
 * 对应 B 组接口文档 §4.1。
 */
@Data
public class RecommendedTripVO {

    private String driverTripId;
    private String driverId;
    private String driverName;
    private VehicleVO vehicle;
    private String start;
    private String end;
    private String route;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private Integer availableSeats;
    private Integer overlap;
    private Integer driverCredit;
    private BigDecimal price;
    private String recommendLevel;
    private Integer score;
    private Integer timeScore;
    private String reason;

    @Data
    public static class VehicleVO {
        private String plate;
        private String model;
        private String color;
    }
}
