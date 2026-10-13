package com.pintu.dto;

import lombok.Data;

/**
 * 车辆视图对象。
 */
@Data
public class VehicleVO {

    private String id;
    private String driverId;
    private String plate;
    private String brand;
    private String model;
    private String color;
    private Integer seats;
    private String vehicleType;
    private String status;
}
