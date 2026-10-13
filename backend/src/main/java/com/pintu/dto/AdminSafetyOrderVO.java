package com.pintu.dto;

import lombok.Data;

@Data
public class AdminSafetyOrderVO {
    private String orderId;
    private String route;
    private String passenger;
    private String driver;
    private String status;
    private LocationPointVO latestLocation;
    private String latestLocationAt;
    private Boolean noLocationUpdate;
    private String safetyStatus;
    private String virtualPhoneStatus;
    private String shareStatus;
    private Long trajectoryCount;
    private Long alertCount;
}
