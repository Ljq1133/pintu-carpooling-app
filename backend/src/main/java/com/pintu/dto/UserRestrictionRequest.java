package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRestrictionRequest {
    @NotBlank
    @Pattern(regexp = "LIMIT_POST|LIMIT_ACCEPT|LIMIT_COMMUNICATION|FULL_BAN", message = "unsupported restriction type")
    private String type;

    @Size(max = 500)
    private String reason;
}
