package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SafetyAlertVO {

    private String id;
    private String orderId;
    private String alertType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String details;
    private String status;
    private String createdAt;
    private String policeReportId;
    private String policeMessage;
}
