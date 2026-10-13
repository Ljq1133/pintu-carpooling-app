package com.pintu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TripShareCreateRequest {
    @Min(5)
    @Max(1440)
    private Integer expiresMinutes;
}
