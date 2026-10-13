package com.pintu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整用户信用分请求体。对应 C 组接口文档 §2.4。
 */
@Data
public class CreditAdjustRequest {

    @NotNull
    private Integer delta;

    private String reason;
}
