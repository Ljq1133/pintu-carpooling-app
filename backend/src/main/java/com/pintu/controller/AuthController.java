package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.LoginRequest;
import com.pintu.dto.LoginResponse;
import com.pintu.dto.ForgotPasswordRequest;
import com.pintu.dto.RefreshTokenRequest;
import com.pintu.dto.RegisterRequest;
import com.pintu.dto.ResetPasswordRequest;
import com.pintu.dto.VerificationCodeLoginRequest;
import com.pintu.dto.VerificationCodeResponse;
import com.pintu.dto.VerificationCodeSendRequest;
import com.pintu.service.UserService;
import com.pintu.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;

    @PostMapping("/login")
    public Result<LoginResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        request.setIpAddress(clientIp(httpRequest));
        return Result.ok(userService.login(request));
    }

    @PostMapping("/login/code")
    public Result<LoginResponse> loginWithCode(
            @RequestBody @Valid VerificationCodeLoginRequest request) {
        return Result.ok(userService.loginWithCode(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@RequestBody @Valid RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/codes")
    public Result<VerificationCodeResponse> sendCode(
            @RequestBody @Valid VerificationCodeSendRequest request) {
        return Result.ok(verificationCodeService.send(request.getPhone(), request.getPurpose()));
    }

    @PostMapping("/password/forgot")
    public Result<VerificationCodeResponse> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        return Result.ok(verificationCodeService.send(request.getPhone(), "RESET_PASSWORD"));
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Result.ok();
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return Result.ok(userService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        userService.logout(request);
        return Result.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
