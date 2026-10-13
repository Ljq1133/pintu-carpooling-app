package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 匹配请求视图对象。
 * 对应 B 组接口文档 §4.3 与 C 组接口文档 §1.5。
 */
@Data
public class MatchRequestVO {

    private String id;
    private String passengerOrderId;
    private String driverTripId;
    private String passengerId;
    private String driverId;
    private String passenger;
    private String route;
    private String time;
    private Integer overlap;
    private Integer credit;
    private BigDecimal price;
    private String status;
    private String initiatorRole;
    private String rejectReason;
    private String orderId;
    private String conversationId;
    private String createdAt;
    private String handledAt;
    private String expiresAt;
}
