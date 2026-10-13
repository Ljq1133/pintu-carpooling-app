package com.pintu.dto;

import lombok.Data;

@Data
public class SupportMessageVO {
    private String id;
    private String sessionId;
    private String senderId;
    private String senderRole;
    private String messageType;
    private String content;
    private String attachmentKey;
    private String clientMessageId;
    private String createdAt;
}
