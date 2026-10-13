package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageRequest {
    @NotBlank
    private String messageType;
    private String content;
    private String attachmentKey;
    private String clientMessageId;
}
