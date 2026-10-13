package com.pintu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleSaveRequest {

    @NotBlank
    @Size(max = 32)
    private String plate;

    @NotBlank
    @Size(max = 64)
    private String brand;

    @NotBlank
    @Size(max = 64)
    private String model;

    @NotBlank
    @Size(max = 32)
    private String color;

    @NotNull
    @Min(1)
    @Max(9)
    private Integer seats;

    @NotBlank
    @Size(max = 32)
    private String vehicleType;

    @NotBlank
    @Size(max = 255)
    private String licenseKey;
}
