package com.pintu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AuditDetailVO {

    private String id;
    private String targetType;
    private String targetId;
    private String name;
    private String plate;
    private String status;
    private String reason;
    private String submittedAt;
    private String handledAt;
    private List<AuditHistoryVO> histories = new ArrayList<>();
    private List<AuditAttachmentVO> attachments = new ArrayList<>();
    private List<AuditNotificationVO> notifications = new ArrayList<>();
}
