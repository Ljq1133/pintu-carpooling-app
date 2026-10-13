package com.pintu.service;

import com.pintu.dto.GeoPointVO;
import com.pintu.dto.PriceEstimateRequest;
import com.pintu.dto.PriceEstimateVO;
import com.pintu.dto.RoutePlanVO;

import java.math.BigDecimal;
import java.util.List;

public interface MapService {
    GeoPointVO locate();

    GeoPointVO reverse(BigDecimal lng, BigDecimal lat);

    List<GeoPointVO> search(String keyword, BigDecimal nearLng, BigDecimal nearLat);

    GeoPointVO geocode(String address);

    RoutePlanVO route(BigDecimal startLng, BigDecimal startLat, BigDecimal endLng, BigDecimal endLat);

    PriceEstimateVO estimate(PriceEstimateRequest request);
}
