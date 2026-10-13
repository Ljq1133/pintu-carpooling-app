package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerificationCodeSendRequest {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    private String phone;

    @NotBlank
    @Pattern(regexp = "REGISTER|LOGIN|RESET_PASSWORD|CHANGE_PHONE", message = "Unsupported purpose")
    private String purpose;
}

