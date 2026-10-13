package com.pintu.dto;

import lombok.Data;

@Data
public class AuditHistoryVO {

    private String id;
    private String adminId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private String createdAt;
}
