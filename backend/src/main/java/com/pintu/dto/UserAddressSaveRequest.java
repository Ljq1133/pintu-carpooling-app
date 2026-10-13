package com.pintu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserAddressSaveRequest {
    private String label;
    @NotBlank
    private String name;
    @NotBlank
    private String address;
    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal lng;
    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal lat;
}
