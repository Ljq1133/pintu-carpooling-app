package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.MatchRequestCreateRequest;
import com.pintu.dto.MatchRequestVO;
import com.pintu.service.MatchRequestService;
import com.pintu.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * 匹配请求接口。对应 B 组接口文档 §4.2 / §4.3 与 C 组接口文档 §1.7。
 */
@RestController
@RequestMapping("/api/match-requests")
@RequiredArgsConstructor
public class MatchRequestController {

    private final MatchRequestService matchRequestService;

    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public Result<MatchRequestVO> create(@RequestBody @Valid MatchRequestCreateRequest request,
                                          Authentication authentication) {
        return Result.ok(matchRequestService.create(request, CurrentUser.id(authentication)));
    }

    @PostMapping("/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public Result<MatchRequestVO> createByDriver(
            @RequestBody @Valid MatchRequestCreateRequest request,
            Authentication authentication) {
        return Result.ok(matchRequestService.createByDriver(request, CurrentUser.id(authentication)));
    }

    @GetMapping("/{id}")
    public Result<MatchRequestVO> get(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(matchRequestService.get(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/accept")
    public Result<MatchRequestVO> accept(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(matchRequestService.accept(id, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/reject")
    public Result<MatchRequestVO> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String reason = body == null ? null : body.get("reason");
        return Result.ok(matchRequestService.reject(id, reason, CurrentUser.id(authentication)));
    }

    @PostMapping("/{id}/withdraw")
    public Result<MatchRequestVO> withdraw(
            @PathVariable String id,
            Authentication authentication) {
        return Result.ok(matchRequestService.withdraw(id, CurrentUser.id(authentication)));
    }
}
