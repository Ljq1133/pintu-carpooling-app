package com.pintu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RuleConfigRequest {
    @NotBlank
    private String ruleType;

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    @NotBlank
    @Size(max = 4000)
    private String content;

    @Min(0)
    @Max(100)
    private Integer grayPercent;
}
