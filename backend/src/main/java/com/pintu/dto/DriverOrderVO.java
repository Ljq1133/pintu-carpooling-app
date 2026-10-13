package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 司机端订单履约视图。
 */
@Data
public class DriverOrderVO {

    private String id;
    private String passengerOrderId;
    private String driverTripId;
    private String passengerId;
    private String passengerName;
    private String vehicle;
    private String start;
    private String end;
    private String route;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private String actualDeparture;
    private String actualArrival;
    private Integer passengerCount;
    private BigDecimal price;
    private String status;
    private String paymentStatus;
    private String reviewStatus;
    private String cancelReason;
}
