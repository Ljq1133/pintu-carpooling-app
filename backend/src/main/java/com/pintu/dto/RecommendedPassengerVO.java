package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendedPassengerVO {
    private String passengerOrderId;
    private String passengerId;
    private String passengerName;
    private String start;
    private String end;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private Integer passengerCount;
    private Integer passengerCredit;
    private Integer overlap;
    private Integer score;
    private String reason;
}
