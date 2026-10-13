package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TripShareViewVO {
    private String orderId;
    private String route;
    private String status;
    private String passenger;
    private String driver;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String departureTime;
    private String expiresAt;
    private List<LocationPointVO> trajectory;
}
