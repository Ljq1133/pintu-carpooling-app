package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmergencyContactSaveRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "must be a valid mobile number")
    private String phone;

    @Size(max = 32)
    private String relationship;

    private Boolean isPrimary;
}
