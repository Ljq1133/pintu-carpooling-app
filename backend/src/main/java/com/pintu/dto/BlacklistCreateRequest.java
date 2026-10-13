package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlacklistCreateRequest {

    @NotBlank
    private String blockedUserId;

    @Size(max = 255)
    private String reason;
}
