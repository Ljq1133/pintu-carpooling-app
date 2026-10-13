package com.pintu.dto;

import lombok.Data;

@Data
public class AuditNotificationVO {
    private String id;
    private String title;
    private String sendStatus;
    private Integer retryCount;
    private String createdAt;
}
