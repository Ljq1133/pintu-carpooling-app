package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportMessageRequest {
    @NotBlank
    private String messageType;

    @Size(max = 1000)
    private String content;

    @Size(max = 255)
    private String attachmentKey;

    @Size(max = 64)
    private String clientMessageId;
}
