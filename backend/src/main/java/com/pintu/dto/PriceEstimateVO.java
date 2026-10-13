package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceEstimateVO {
    private BigDecimal basePrice;
    private BigDecimal distancePrice;
    private BigDecimal durationPrice;
    private BigDecimal passengerPrice;
    private BigDecimal distanceKm;
    private Integer durationMinutes;
    private Integer passengerCount;
    private BigDecimal total;
    private String currency;
}
