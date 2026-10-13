package com.pintu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模拟付款请求体。对应 B 组接口文档 §5.2。
 */
@Data
public class PaymentRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    private String method;
}
