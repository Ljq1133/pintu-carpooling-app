package com.pintu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DataRetentionPolicyRequest {
    @NotBlank
    private String dataType;

    @Min(1)
    private Integer retentionDays;

    @Pattern(regexp = "ARCHIVE_THEN_DELETE|DELETE_OBJECT|KEEP_AUDIT", message = "unsupported deleteMode")
    private String deleteMode;

    @Size(max = 500)
    private String description;
}
