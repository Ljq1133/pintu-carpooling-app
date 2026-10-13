package com.pintu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建司机行程请求体。
 * 对应 C 组接口文档 §1.3。
 */
@Data
public class DriverTripCreateRequest {

    @NotBlank
    private String start;

    @NotBlank
    private String end;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal startLng;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal startLat;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal endLng;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal endLat;

    @NotNull
    private LocalDateTime departureTime;

    @NotBlank
    private String vehicleId;

    @NotNull
    @Positive
    private Integer availableSeats;

    private BigDecimal price;

    /** 演示场景指定司机 ID。 */
    private String driverId;
}
