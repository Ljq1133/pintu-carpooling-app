package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.AppealCreateRequest;
import com.pintu.dto.AppealTicketVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/passenger")
@RequiredArgsConstructor
public class PassengerAppealController {

    private final AppealService appealService;

    @PostMapping("/orders/{orderId}/appeals")
    public Result<AppealTicketVO> create(
            Authentication authentication,
            @PathVariable String orderId,
            @RequestBody @Valid AppealCreateRequest request) {
        return Result.ok(appealService.createPassengerAppeal(CurrentUser.id(authentication), orderId, request));
    }

    @GetMapping("/appeals")
    public Result<List<AppealTicketVO>> list(Authentication authentication) {
        return Result.ok(appealService.listPassengerAppeals(CurrentUser.id(authentication)));
    }

    @GetMapping("/appeals/{appealId}")
    public Result<AppealTicketVO> detail(Authentication authentication, @PathVariable String appealId) {
        return Result.ok(appealService.getPassengerAppeal(CurrentUser.id(authentication), appealId));
    }
}
