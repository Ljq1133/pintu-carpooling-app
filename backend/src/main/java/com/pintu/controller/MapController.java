package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.GeoPointVO;
import com.pintu.dto.PriceEstimateRequest;
import com.pintu.dto.PriceEstimateVO;
import com.pintu.dto.RoutePlanVO;
import com.pintu.service.MapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {
    private final MapService mapService;

    @GetMapping("/location")
    public Result<GeoPointVO> location() {
        return Result.ok(mapService.locate());
    }

    @GetMapping("/reverse")
    public Result<GeoPointVO> reverse(@RequestParam BigDecimal lng, @RequestParam BigDecimal lat) {
        return Result.ok(mapService.reverse(lng, lat));
    }

    @GetMapping("/pois")
    public Result<List<GeoPointVO>> pois(
            @RequestParam String keyword,
            @RequestParam(required = false) BigDecimal nearLng,
            @RequestParam(required = false) BigDecimal nearLat) {
        return Result.ok(mapService.search(keyword, nearLng, nearLat));
    }

    @GetMapping("/route")
    public Result<RoutePlanVO> route(
            @RequestParam BigDecimal startLng,
            @RequestParam BigDecimal startLat,
            @RequestParam BigDecimal endLng,
            @RequestParam BigDecimal endLat) {
        return Result.ok(mapService.route(startLng, startLat, endLng, endLat));
    }

    @PostMapping("/price-estimate")
    public Result<PriceEstimateVO> estimate(@RequestBody @Valid PriceEstimateRequest request) {
        return Result.ok(mapService.estimate(request));
    }
}
