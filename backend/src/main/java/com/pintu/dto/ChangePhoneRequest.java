package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePhoneRequest {

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number")
    private String newPhone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Code must contain 6 digits")
    private String code;
}

