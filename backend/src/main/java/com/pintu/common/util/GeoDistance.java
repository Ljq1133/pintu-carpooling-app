package com.pintu.common.util;

import java.math.BigDecimal;

public final class GeoDistance {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoDistance() {
    }

    public static double km(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        if (lng1 == null || lat1 == null || lng2 == null || lat2 == null) return Double.MAX_VALUE;
        double latDelta = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lngDelta = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
