package com.pintu.service.map;

import com.pintu.dto.GeoPointVO;
import com.pintu.dto.RoutePlanVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "map.provider", havingValue = "mock", matchIfMissing = true)
public class MockMapProvider implements MapProvider {
    private static final BigDecimal DEFAULT_LNG = new BigDecimal("121.5063770");
    private static final BigDecimal DEFAULT_LAT = new BigDecimal("31.2820800");
    private static final Map<String, GeoPointVO> POIS = createPois();

    @Override
    public GeoPointVO locate() {
        return new GeoPointVO("当前位置", "上海市杨浦区同济科技园", DEFAULT_LNG, DEFAULT_LAT);
    }

    @Override
    public GeoPointVO reverseGeocode(BigDecimal lng, BigDecimal lat) {
        return POIS.values().stream()
                .min((left, right) -> Double.compare(distanceKm(lng, lat, left.getLng(), left.getLat()),
                        distanceKm(lng, lat, right.getLng(), right.getLat())))
                .map(point -> new GeoPointVO(point.getName(), point.getAddress(), lng, lat))
                .orElseGet(() -> new GeoPointVO("地图选点", coordinateAddress(lng, lat), lng, lat));
    }

    @Override
    public List<GeoPointVO> searchPoi(String keyword, BigDecimal nearLng, BigDecimal nearLat) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<GeoPointVO> matches = POIS.values().stream()
                .filter(point -> normalized.isEmpty()
                        || point.getName().toLowerCase(Locale.ROOT).contains(normalized)
                        || point.getAddress().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(8)
                .map(this::copy)
                .toList();
        if (!matches.isEmpty()) return matches;
        return List.of(geocode(keyword));
    }

    @Override
    public GeoPointVO geocode(String address) {
        String normalized = address == null ? "" : address.trim();
        for (Map.Entry<String, GeoPointVO> entry : POIS.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return copy(entry.getValue());
            }
        }
        int hash = Math.abs(normalized.hashCode());
        BigDecimal lng = DEFAULT_LNG.add(BigDecimal.valueOf((hash % 2400 - 1200) / 100000.0));
        BigDecimal lat = DEFAULT_LAT.add(BigDecimal.valueOf(((hash / 2400) % 1800 - 900) / 100000.0));
        return new GeoPointVO(normalized.isBlank() ? "地图选点" : normalized,
                "上海市 · " + (normalized.isBlank() ? "模拟位置" : normalized), lng, lat);
    }

    @Override
    public RoutePlanVO planRoute(GeoPointVO origin, GeoPointVO destination) {
        double straightDistance = distanceKm(
                origin.getLng(), origin.getLat(), destination.getLng(), destination.getLat());
        BigDecimal roadDistance = BigDecimal.valueOf(straightDistance * 1.22)
                .setScale(2, RoundingMode.HALF_UP);
        int duration = Math.max(1, (int) Math.ceil(roadDistance.doubleValue() / 30.0 * 60));
        RoutePlanVO route = new RoutePlanVO();
        route.setOrigin(copy(origin));
        route.setDestination(copy(destination));
        route.setDistanceKm(roadDistance);
        route.setDurationMinutes(duration);
        route.setPolyline(List.of(copy(origin), midpoint(origin, destination), copy(destination)));
        route.setProvider(name());
        route.setDegraded(false);
        return route;
    }

    @Override
    public String name() {
        return "mock-map";
    }

    private GeoPointVO midpoint(GeoPointVO origin, GeoPointVO destination) {
        BigDecimal lng = origin.getLng().add(destination.getLng())
                .divide(BigDecimal.valueOf(2), 7, RoundingMode.HALF_UP);
        BigDecimal lat = origin.getLat().add(destination.getLat())
                .divide(BigDecimal.valueOf(2), 7, RoundingMode.HALF_UP);
        return new GeoPointVO("途经点", coordinateAddress(lng, lat), lng, lat);
    }

    private GeoPointVO copy(GeoPointVO point) {
        return new GeoPointVO(point.getName(), point.getAddress(), point.getLng(), point.getLat());
    }

    private String coordinateAddress(BigDecimal lng, BigDecimal lat) {
        return "上海市模拟坐标 " + lng + "," + lat;
    }

    private double distanceKm(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        double earthRadius = 6371.0088;
        double latDelta = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lngDelta = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static Map<String, GeoPointVO> createPois() {
        Map<String, GeoPointVO> result = new LinkedHashMap<>();
        result.put("同济科技园", new GeoPointVO("同济科技园", "上海市杨浦区赤峰路65号",
                new BigDecimal("121.5063770"), new BigDecimal("31.2820800")));
        result.put("中央科技园", new GeoPointVO("中央科技园", "上海市杨浦区国康路100号",
                new BigDecimal("121.5008000"), new BigDecimal("31.2846000")));
        result.put("五角场", new GeoPointVO("五角场", "上海市杨浦区五角场商圈",
                new BigDecimal("121.5146500"), new BigDecimal("31.2993200")));
        result.put("SOHO", new GeoPointVO("虹口SOHO", "上海市虹口区四川北路",
                new BigDecimal("121.4896200"), new BigDecimal("31.2582100")));
        result.put("陆家嘴", new GeoPointVO("陆家嘴中心", "上海市浦东新区陆家嘴",
                new BigDecimal("121.5012600"), new BigDecimal("31.2383600")));
        result.put("人民广场", new GeoPointVO("人民广场", "上海市黄浦区人民大道",
                new BigDecimal("121.4737000"), new BigDecimal("31.2304000")));
        return result;
    }
}
