package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoutePlanVO {
    private GeoPointVO origin;
    private GeoPointVO destination;
    private BigDecimal distanceKm;
    private Integer durationMinutes;
    private List<GeoPointVO> polyline;
    private String provider;
    private boolean degraded;
}
