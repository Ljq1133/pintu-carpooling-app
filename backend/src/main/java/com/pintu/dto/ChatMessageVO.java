package com.pintu.dto;

import lombok.Data;

@Data
public class ChatMessageVO {
    private String id;
    private String conversationId;
    private String senderId;
    private String messageType;
    private String content;
    private String attachmentKey;
    private String clientMessageId;
    private String readAt;
    private String createdAt;
}
