package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SafetyAlertCreateRequest {

    @NotBlank
    @Size(max = 32)
    private String alertType;

    private BigDecimal longitude;
    private BigDecimal latitude;

    @Size(max = 1000)
    private String details;
}
