package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 乘客拼车需求视图对象。
 * 对应 B 组接口文档 §3.1 / §3.2 / §3.3。
 */
@Data
public class PassengerOrderVO {

    private String id;
    private String passengerOrderId;
    private String orderId;

    private String passengerId;
    private String passengerName;
    private String start;
    private String end;
    private String route;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private Integer passengerCount;
    private String note;

    private String driverId;
    private String driverName;
    private String driverPhone;
    private String vehicle;
    private BigDecimal price;

    private String status;
    private String paymentStatus;
    private String reviewStatus;
    private String cancelReason;
    private String summary;
    private String createdAt;
}
