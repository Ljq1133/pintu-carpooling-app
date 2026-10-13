package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AccountBindingRequest {
    @NotBlank
    @Pattern(regexp = "PHONE|PAYMENT|SOCIAL", message = "bindingType must be PHONE, PAYMENT or SOCIAL")
    private String bindingType;

    @Size(max = 64)
    private String provider;

    @Size(max = 128)
    private String accountLabel;

    @NotBlank
    @Size(max = 255)
    private String accountValue;
}
