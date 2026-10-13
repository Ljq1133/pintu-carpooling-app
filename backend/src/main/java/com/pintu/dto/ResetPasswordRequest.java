package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Code must contain 6 digits")
    private String code;

    @NotBlank
    @Size(min = 8, max = 64)
    private String newPassword;
}

