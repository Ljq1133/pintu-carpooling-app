package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppealTransitionRequest {
    @NotBlank
    @Size(max = 32)
    private String status;

    @Size(max = 1000)
    private String comment;

    @Size(max = 32)
    private String assigneeId;
}
