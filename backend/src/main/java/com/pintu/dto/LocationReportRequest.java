package com.pintu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LocationReportRequest {
    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal longitude;

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal latitude;

    private BigDecimal accuracy;
    private BigDecimal speed;
    private BigDecimal heading;
    @Size(max = 64)
    private String clientReportId;
    @Min(1)
    @Max(10)
    private Integer reportIntervalSeconds;
    @Min(0)
    private Integer retryCount;
    private LocalDateTime reportedAt;
}
