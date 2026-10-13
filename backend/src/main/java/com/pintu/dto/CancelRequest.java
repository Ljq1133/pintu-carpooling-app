package com.pintu.dto;

import lombok.Data;

/**
 * 取消订单 / 需求请求体。对应 B 组接口文档 §3.5。
 */
@Data
public class CancelRequest {

    private String reason;
}
