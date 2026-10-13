package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationPointVO {
    private String id;
    private String orderId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal accuracy;
    private BigDecimal speed;
    private BigDecimal heading;
    private Integer sequenceNo;
    private String reportedAt;
}
