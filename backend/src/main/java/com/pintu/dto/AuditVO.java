package com.pintu.dto;

import lombok.Data;

/**
 * 审核记录视图对象。对应 C 组接口文档 §2.1 / §2.2。
 */
@Data
public class AuditVO {

    private String id;
    private String targetType;
    private String targetId;
    private String name;
    private String plate;
    private String status;
    private String reason;
    private String submittedAt;
    private String handledAt;
}
