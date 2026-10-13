package com.pintu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NegotiationProposalRequest {
    @NotNull
    @DecimalMin("0")
    private BigDecimal price;
    @NotNull
    @Future
    private LocalDateTime departureTime;
    @NotNull
    @Min(1)
    @Max(8)
    private Integer passengerCount;
    @Size(max = 128)
    private String pickupLocation;
    private BigDecimal pickupLng;
    private BigDecimal pickupLat;
    @Size(max = 500)
    private String note;
}
