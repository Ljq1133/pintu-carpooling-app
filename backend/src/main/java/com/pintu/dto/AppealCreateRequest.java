package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AppealCreateRequest {
    @NotBlank
    @Size(max = 32)
    private String reasonType;

    @Size(max = 2000)
    private String description;

    private List<@Size(max = 255) String> evidenceKeys;
}
