package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.DriverTripCreateRequest;
import com.pintu.dto.DriverTripVO;
import com.pintu.dto.DriverOrderVO;
import com.pintu.dto.MatchRequestVO;
import com.pintu.dto.UserVO;
import com.pintu.dto.VehicleVO;
import com.pintu.dto.VehicleSaveRequest;
import com.pintu.dto.RecommendedPassengerVO;
import com.pintu.service.OrderService;
import com.pintu.service.DriverService;
import com.pintu.service.MatchRequestService;
import com.pintu.service.UserService;
import com.pintu.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.core.Authentication;

/**
 * 司机端接口。对应 C 组接口文档 §1。
 */
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final UserService userService;
    private final MatchRequestService matchRequestService;
    private final OrderService orderService;

    @GetMapping("/profile")
    public Result<UserVO> profile(Authentication authentication) {
        return Result.ok(userService.getDriverProfile(CurrentUser.id(authentication)));
    }

    @GetMapping("/vehicles")
    public Result<List<VehicleVO>> vehicles(Authentication authentication) {
        return Result.ok(driverService.listVehicles(CurrentUser.id(authentication)));
    }

    @GetMapping("/vehicles/{vehicleId}")
    public Result<VehicleVO> vehicle(
            @PathVariable String vehicleId,
            Authentication authentication) {
        return Result.ok(driverService.getVehicle(CurrentUser.id(authentication), vehicleId));
    }

    @PostMapping("/vehicles")
    public Result<VehicleVO> createVehicle(
            @RequestBody @Valid VehicleSaveRequest request,
            Authentication authentication) {
        return Result.ok(driverService.createVehicle(CurrentUser.id(authentication), request));
    }

    @PutMapping("/vehicles/{vehicleId}")
    public Result<VehicleVO> updateVehicle(
            @PathVariable String vehicleId,
            @RequestBody @Valid VehicleSaveRequest request,
            Authentication authentication) {
        return Result.ok(driverService.updateVehicle(
                CurrentUser.id(authentication), vehicleId, request));
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    public Result<Void> deleteVehicle(
            @PathVariable String vehicleId,
            Authentication authentication) {
        driverService.deleteVehicle(CurrentUser.id(authentication), vehicleId);
        return Result.ok();
    }

    @PostMapping("/trips")
    public Result<DriverTripVO> createTrip(@RequestBody @Valid DriverTripCreateRequest request,
                                            Authentication authentication) {
        request.setDriverId(CurrentUser.id(authentication));
        return Result.ok(driverService.createTrip(request));
    }

    @GetMapping("/trips")
    public Result<List<DriverTripVO>> listTrips(Authentication authentication) {
        return Result.ok(driverService.listTrips(CurrentUser.id(authentication)));
    }

    @GetMapping("/trips/{tripId}/matches")
    public Result<List<MatchRequestVO>> tripMatches(
            @PathVariable String tripId,
            Authentication authentication) {
        return Result.ok(matchRequestService.listByDriverTrip(tripId, CurrentUser.id(authentication)));
    }

    @GetMapping("/trips/{tripId}/recommendations")
    public Result<List<RecommendedPassengerVO>> tripRecommendations(
            @PathVariable String tripId,
            Authentication authentication) {
        return Result.ok(driverService.recommendPassengers(CurrentUser.id(authentication), tripId));
    }

    @GetMapping("/orders")
    public Result<List<DriverOrderVO>> orders(
            Authentication authentication) {
        return Result.ok(orderService.listByDriver(CurrentUser.id(authentication)));
    }
}
