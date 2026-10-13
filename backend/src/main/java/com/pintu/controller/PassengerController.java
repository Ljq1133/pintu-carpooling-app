package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.CancelRequest;
import com.pintu.dto.PassengerOrderCreateRequest;
import com.pintu.dto.PassengerOrderVO;
import com.pintu.dto.RecommendedTripVO;
import com.pintu.dto.UserVO;
import com.pintu.service.PassengerOrderService;
import com.pintu.service.UserService;
import com.pintu.service.MatchRequestService;
import com.pintu.dto.MatchRequestVO;
import com.pintu.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.core.Authentication;

/**
 * 乘客端接口。对应 B 组接口文档 §2 / §3 / §4。
 */
@RestController
@RequestMapping("/api/passenger")
@RequiredArgsConstructor
public class PassengerController {

    private final UserService userService;
    private final PassengerOrderService passengerOrderService;
    private final MatchRequestService matchRequestService;

    @GetMapping("/profile")
    public Result<UserVO> profile(Authentication authentication) {
        return Result.ok(userService.getPassengerProfile(CurrentUser.id(authentication)));
    }

    @PostMapping("/orders")
    public Result<PassengerOrderVO> create(@RequestBody @Valid PassengerOrderCreateRequest request,
                                            Authentication authentication) {
        request.setPassengerId(CurrentUser.id(authentication));
        return Result.ok(passengerOrderService.create(request));
    }

    @GetMapping("/orders")
    public Result<List<PassengerOrderVO>> list(Authentication authentication,
                                               @RequestParam(required = false) String status) {
        return Result.ok(passengerOrderService.listByPassenger(CurrentUser.id(authentication), status));
    }

    @GetMapping("/orders/{id}")
    public Result<PassengerOrderVO> detail(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(passengerOrderService.detail(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/orders/{id}/cancel")
    public Result<PassengerOrderVO> cancel(
            @PathVariable String id,
            @RequestBody(required = false) CancelRequest request,
            Authentication authentication) {
        return Result.ok(passengerOrderService.cancel(id, request, CurrentUser.id(authentication)));
    }

    @GetMapping("/orders/{passengerOrderId}/matches")
    public Result<List<RecommendedTripVO>> matches(
            @PathVariable String passengerOrderId,
            Authentication authentication) {
        return Result.ok(passengerOrderService.recommend(passengerOrderId, CurrentUser.id(authentication)));
    }

    @GetMapping("/orders/{passengerOrderId}/requests")
    public Result<List<MatchRequestVO>> requests(
            @PathVariable String passengerOrderId,
            Authentication authentication) {
        return Result.ok(matchRequestService.listByPassengerOrder(
                passengerOrderId, CurrentUser.id(authentication)));
    }
}
