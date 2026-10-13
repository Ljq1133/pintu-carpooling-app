package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建匹配请求请求体。
 * 对应 B 组接口文档 §4.2。
 */
@Data
public class MatchRequestCreateRequest {

    @NotBlank
    private String passengerOrderId;

    @NotBlank
    private String driverTripId;

    private String message;
}
