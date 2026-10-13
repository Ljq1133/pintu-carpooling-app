package com.pintu.service.impl;

import com.pintu.common.exception.BusinessException;
import com.pintu.dto.GeoPointVO;
import com.pintu.dto.PriceEstimateRequest;
import com.pintu.dto.PriceEstimateVO;
import com.pintu.dto.RoutePlanVO;
import com.pintu.service.MapService;
import com.pintu.service.RuleRuntimeService;
import com.pintu.service.map.MapProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class MapServiceImpl implements MapService {
    private final MapProvider provider;
    private final RuleRuntimeService ruleRuntimeService;
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final Duration cacheTtl;
    private final int maxAttempts;
    private final long timeoutMs;
    private final BigDecimal basePrice;
    private final BigDecimal distanceRate;
    private final BigDecimal durationRate;
    private final BigDecimal extraPassengerRate;

    public MapServiceImpl(
            MapProvider provider,
            RuleRuntimeService ruleRuntimeService,
            @Value("${map.cache-ttl-seconds:300}") long cacheTtlSeconds,
            @Value("${map.max-attempts:2}") int maxAttempts,
            @Value("${map.timeout-ms:1500}") long timeoutMs,
            @Value("${pricing.base-price:6.00}") BigDecimal basePrice,
            @Value("${pricing.distance-rate:1.80}") BigDecimal distanceRate,
            @Value("${pricing.duration-rate:0.25}") BigDecimal durationRate,
            @Value("${pricing.extra-passenger-rate:2.00}") BigDecimal extraPassengerRate) {
        this.provider = provider;
        this.ruleRuntimeService = ruleRuntimeService;
        this.cacheTtl = Duration.ofSeconds(Math.max(1, cacheTtlSeconds));
        this.maxAttempts = Math.max(1, maxAttempts);
        this.timeoutMs = Math.max(100, timeoutMs);
        this.basePrice = basePrice;
        this.distanceRate = distanceRate;
        this.durationRate = durationRate;
        this.extraPassengerRate = extraPassengerRate;
    }

    @Override
    public GeoPointVO locate() {
        return invoke("location", provider::locate);
    }

    @Override
    public GeoPointVO reverse(BigDecimal lng, BigDecimal lat) {
        requireCoordinates(lng, lat);
        return invoke("reverse:" + lng + ":" + lat, () -> provider.reverseGeocode(lng, lat));
    }

    @Override
    public List<GeoPointVO> search(String keyword, BigDecimal nearLng, BigDecimal nearLat) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(1002, "POI 搜索关键词不能为空");
        }
        return invoke("search:" + keyword.trim() + ":" + nearLng + ":" + nearLat,
                () -> provider.searchPoi(keyword.trim(), nearLng, nearLat));
    }

    @Override
    public GeoPointVO geocode(String address) {
        if (address == null || address.isBlank()) {
            throw new BusinessException(1002, "地址不能为空");
        }
        return invoke("geocode:" + address.trim(), () -> provider.geocode(address.trim()));
    }

    @Override
    public RoutePlanVO route(BigDecimal startLng, BigDecimal startLat,
                             BigDecimal endLng, BigDecimal endLat) {
        requireCoordinates(startLng, startLat);
        requireCoordinates(endLng, endLat);
        String key = "route:" + startLng + ":" + startLat + ":" + endLng + ":" + endLat;
        return invoke(key, () -> provider.planRoute(
                new GeoPointVO("起点", null, startLng, startLat),
                new GeoPointVO("终点", null, endLng, endLat)));
    }

    @Override
    public PriceEstimateVO estimate(PriceEstimateRequest request) {
        RoutePlanVO route = route(request.getStartLng(), request.getStartLat(),
                request.getEndLng(), request.getEndLat());
        BigDecimal effectiveBasePrice = ruleRuntimeService.decimalValue("PRICING", "DEFAULT", "basePrice", basePrice);
        BigDecimal effectiveDistanceRate = ruleRuntimeService.decimalValue("PRICING", "DEFAULT", "distanceRate", distanceRate);
        BigDecimal effectiveDurationRate = ruleRuntimeService.decimalValue("PRICING", "DEFAULT", "durationRate", durationRate);
        BigDecimal effectiveExtraPassengerRate = ruleRuntimeService.decimalValue(
                "PRICING", "DEFAULT", "extraPassengerRate", extraPassengerRate);
        BigDecimal distancePrice = route.getDistanceKm().multiply(effectiveDistanceRate);
        BigDecimal durationPrice = BigDecimal.valueOf(route.getDurationMinutes()).multiply(effectiveDurationRate);
        BigDecimal passengerPrice = BigDecimal.valueOf(Math.max(0, request.getPassengerCount() - 1L))
                .multiply(effectiveExtraPassengerRate);

        PriceEstimateVO result = new PriceEstimateVO();
        result.setBasePrice(money(effectiveBasePrice));
        result.setDistancePrice(money(distancePrice));
        result.setDurationPrice(money(durationPrice));
        result.setPassengerPrice(money(passengerPrice));
        result.setDistanceKm(route.getDistanceKm());
        result.setDurationMinutes(route.getDurationMinutes());
        result.setPassengerCount(request.getPassengerCount());
        result.setTotal(money(effectiveBasePrice.add(distancePrice).add(durationPrice).add(passengerPrice)));
        result.setCurrency("CNY");
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String key, Supplier<T> action) {
        CacheEntry<?> cached = cache.get(key);
        if (cached != null && !cached.expired(cacheTtl)) return (T) cached.value();

        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T value = CompletableFuture.supplyAsync(action)
                        .get(timeoutMs, TimeUnit.MILLISECONDS);
                cache.put(key, new CacheEntry<>(value, Instant.now()));
                return value;
            } catch (Exception exception) {
                last = exception instanceof RuntimeException runtime
                        ? runtime
                        : new IllegalStateException(exception.getMessage(), exception);
            }
        }
        if (cached != null) return (T) cached.value();
        throw new BusinessException(1503, "地图服务暂时不可用：" + (last == null ? "unknown" : last.getMessage()));
    }

    private void requireCoordinates(BigDecimal lng, BigDecimal lat) {
        if (lng == null || lat == null
                || lng.compareTo(BigDecimal.valueOf(-180)) < 0
                || lng.compareTo(BigDecimal.valueOf(180)) > 0
                || lat.compareTo(BigDecimal.valueOf(-90)) < 0
                || lat.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BusinessException(1002, "经纬度不合法");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record CacheEntry<T>(T value, Instant storedAt) {
        private boolean expired(Duration ttl) {
            return storedAt.plus(ttl).isBefore(Instant.now());
        }
    }
}
