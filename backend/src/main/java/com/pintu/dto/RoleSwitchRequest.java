package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RoleSwitchRequest {

    @NotBlank
    @Pattern(regexp = "passenger|driver|admin")
    private String role;
}

