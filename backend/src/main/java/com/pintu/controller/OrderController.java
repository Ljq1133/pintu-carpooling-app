package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.PaymentRequest;
import com.pintu.dto.PaymentVO;
import com.pintu.dto.ReviewRequest;
import com.pintu.dto.ReviewVO;
import com.pintu.entity.OrderRecord;
import com.pintu.service.OrderService;
import com.pintu.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 正式订单接口。
 * 对应 B 组接口文档 §5 与 C 组接口文档 §1.8 / §1.9。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<OrderRecord> start(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(orderService.startTrip(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/arrive")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<OrderRecord> arrive(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(orderService.arrive(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('PASSENGER')")
    public Result<PaymentVO> pay(
            @PathVariable String id,
            @RequestBody @Valid PaymentRequest request,
            Authentication authentication) {
        return Result.ok(orderService.pay(id, request, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('PASSENGER')")
    public Result<ReviewVO> review(
            @PathVariable String id,
            @RequestBody @Valid ReviewRequest request,
            Authentication authentication) {
        return Result.ok(orderService.review(id, request, CurrentUser.id(authentication)));
    }
}
