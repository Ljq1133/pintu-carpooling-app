package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.SupportMessageRequest;
import com.pintu.dto.SupportMessageVO;
import com.pintu.dto.SupportSessionCreateRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support/sessions")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping
    public Result<List<SupportSessionVO>> sessions(Authentication authentication) {
        return Result.ok(supportService.listUserSessions(CurrentUser.id(authentication)));
    }

    @PostMapping
    public Result<SupportSessionVO> create(
            Authentication authentication,
            @RequestBody @Valid SupportSessionCreateRequest request) {
        return Result.ok(supportService.createUserSession(CurrentUser.id(authentication), request));
    }

    @GetMapping("/{id}/messages")
    public Result<List<SupportMessageVO>> messages(
            Authentication authentication,
            @PathVariable String id) {
        return Result.ok(supportService.listUserMessages(CurrentUser.id(authentication), id));
    }

    @PostMapping("/{id}/messages")
    public Result<SupportMessageVO> send(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody @Valid SupportMessageRequest request) {
        return Result.ok(supportService.sendUserMessage(CurrentUser.id(authentication), id, request));
    }
}
