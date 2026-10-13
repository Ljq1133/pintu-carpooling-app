package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.BlacklistCreateRequest;
import com.pintu.dto.BlacklistVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.PrivacyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/privacy/blacklist")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    @GetMapping
    public Result<List<BlacklistVO>> list(Authentication authentication) {
        return Result.ok(privacyService.listBlockedUsers(CurrentUser.id(authentication)));
    }

    @PostMapping
    public Result<BlacklistVO> block(
            Authentication authentication,
            @RequestBody @Valid BlacklistCreateRequest request) {
        return Result.ok(privacyService.blockUser(CurrentUser.id(authentication), request));
    }

    @DeleteMapping("/{blockedUserId}")
    public Result<Void> unblock(
            Authentication authentication,
            @PathVariable String blockedUserId) {
        privacyService.unblockUser(CurrentUser.id(authentication), blockedUserId);
        return Result.ok();
    }
}
