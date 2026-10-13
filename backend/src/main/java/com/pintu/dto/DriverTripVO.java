package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 司机行程视图对象。
 */
@Data
public class DriverTripVO {

    private String id;
    private String driverId;
    private String driverName;
    private String vehicleId;
    private String vehicle;
    private String start;
    private String end;
    private String route;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private Integer availableSeats;
    private BigDecimal price;
    private Integer overlap;
    private Integer driverCredit;
    private String status;
}
