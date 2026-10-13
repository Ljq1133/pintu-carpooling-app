package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DriverIdentityApplyRequest {

    @NotBlank
    private String realName;

    @NotBlank
    @Pattern(regexp = "^(\\d{15}|\\d{17}[0-9Xx])$", message = "Invalid ID card number")
    private String idCard;

    @NotBlank
    private String idCardFrontKey;

    @NotBlank
    private String idCardBackKey;

    @NotBlank
    private String livenessResult;
}

