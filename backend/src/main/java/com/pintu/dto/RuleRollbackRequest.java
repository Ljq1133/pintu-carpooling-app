package com.pintu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RuleRollbackRequest {
    @NotNull
    @Min(1)
    private Integer toVersion;

    @Size(max = 500)
    private String reason;
}
