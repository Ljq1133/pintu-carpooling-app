package com.pintu.dto;

import lombok.Data;

/**
 * 通用审核审批请求体（驳回时填写 reason）。
 */
@Data
public class AuditDecisionRequest {

    private String reason;
}
