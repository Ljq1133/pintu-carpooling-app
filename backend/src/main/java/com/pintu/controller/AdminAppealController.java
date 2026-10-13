package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.AppealTicketVO;
import com.pintu.dto.AppealTransitionRequest;
import com.pintu.dto.ArbitrationRequest;
import com.pintu.dto.ArbitrationVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/appeals")
@RequiredArgsConstructor
public class AdminAppealController {

    private final AppealService appealService;

    @GetMapping
    public Result<List<AppealTicketVO>> list(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        return Result.ok(appealService.listAdminAppeals(CurrentUser.id(authentication), status));
    }

    @GetMapping("/{appealId}")
    public Result<AppealTicketVO> detail(Authentication authentication, @PathVariable String appealId) {
        return Result.ok(appealService.getAdminAppeal(CurrentUser.id(authentication), appealId));
    }

    @PostMapping("/{appealId}/transition")
    public Result<AppealTicketVO> transition(
            Authentication authentication,
            @PathVariable String appealId,
            @RequestBody @Valid AppealTransitionRequest request) {
        return Result.ok(appealService.transitionByAdmin(CurrentUser.id(authentication), appealId, request));
    }

    @PostMapping("/{appealId}/arbitrations")
    public Result<ArbitrationVO> arbitrate(
            Authentication authentication,
            @PathVariable String appealId,
            @RequestBody @Valid ArbitrationRequest request) {
        return Result.ok(appealService.arbitrate(CurrentUser.id(authentication), appealId, request));
    }
}
