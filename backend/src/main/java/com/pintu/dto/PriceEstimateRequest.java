package com.pintu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceEstimateRequest {
    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal startLng;

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal startLat;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal endLng;

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal endLat;

    @NotNull
    @Positive
    private Integer passengerCount;
}
