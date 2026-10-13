package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArbitrationVO {
    private String id;
    private String appealId;
    private String adminId;
    private String actionType;
    private BigDecimal amount;
    private Integer creditDelta;
    private String remark;
    private String confirmedBy;
    private String createdAt;
}
