package com.pintu.service.map;

import com.pintu.dto.GeoPointVO;
import com.pintu.dto.RoutePlanVO;

import java.math.BigDecimal;
import java.util.List;

public interface MapProvider {
    GeoPointVO locate();

    GeoPointVO reverseGeocode(BigDecimal lng, BigDecimal lat);

    List<GeoPointVO> searchPoi(String keyword, BigDecimal nearLng, BigDecimal nearLat);

    GeoPointVO geocode(String address);

    RoutePlanVO planRoute(GeoPointVO origin, GeoPointVO destination);

    String name();
}
