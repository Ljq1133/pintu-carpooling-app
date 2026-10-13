package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.SupportMessageRequest;
import com.pintu.dto.SupportMessageVO;
import com.pintu.dto.SupportSessionVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.SupportService;
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
@RequestMapping("/api/admin/support/sessions")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping
    public Result<List<SupportSessionVO>> sessions(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        return Result.ok(supportService.listAdminSessions(CurrentUser.id(authentication), status));
    }

    @PostMapping("/{id}/claim")
    public Result<SupportSessionVO> claim(@PathVariable String id, Authentication authentication) {
        return Result.ok(supportService.claim(CurrentUser.id(authentication), id));
    }

    @PostMapping("/{id}/messages")
    public Result<SupportMessageVO> send(
            @PathVariable String id,
            @RequestBody @Valid SupportMessageRequest request,
            Authentication authentication) {
        return Result.ok(supportService.sendAdminMessage(CurrentUser.id(authentication), id, request));
    }

    @PostMapping("/{id}/close")
    public Result<SupportSessionVO> close(@PathVariable String id, Authentication authentication) {
        return Result.ok(supportService.close(CurrentUser.id(authentication), id));
    }
}
