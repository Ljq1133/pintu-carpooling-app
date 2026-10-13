package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模拟付款响应。对应 B 组接口文档 §5.2。
 */
@Data
public class PaymentVO {

    private String id;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String orderStatus;
    private String paymentStatus;
    private String reviewStatus;
    private String paidAt;
}
