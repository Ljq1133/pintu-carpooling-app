package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificationCodeLoginRequest {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Code must contain 6 digits")
    private String code;
}

