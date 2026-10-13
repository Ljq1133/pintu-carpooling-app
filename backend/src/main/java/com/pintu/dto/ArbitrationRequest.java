package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArbitrationRequest {
    @NotBlank
    @Size(max = 32)
    private String actionType;

    private BigDecimal amount;
    private Integer creditDelta;

    @Size(max = 32)
    private String targetUserId;

    @Size(max = 1000)
    private String remark;

    private Boolean confirmed;
}
