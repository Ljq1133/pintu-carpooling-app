package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端订单看板项。
 * 对应 C 组接口文档 §2.3。
 */
@Data
public class AdminOrderVO {

    private String id;
    private String route;
    private String passenger;
    private String driver;
    private String status;
    private String paymentStatus;
    private BigDecimal price;
    private String risk;
    private String createdAt;
}
