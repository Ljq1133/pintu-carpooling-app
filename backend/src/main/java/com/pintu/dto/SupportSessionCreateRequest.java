package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportSessionCreateRequest {
    @NotBlank
    @Size(max = 128)
    private String subject;

    @Size(max = 1000)
    private String firstMessage;

    @Size(max = 255)
    private String attachmentKey;
}
