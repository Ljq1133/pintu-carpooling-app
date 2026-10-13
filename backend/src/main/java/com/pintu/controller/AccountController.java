package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.AccountBindingRequest;
import com.pintu.dto.AccountBindingVO;
import com.pintu.dto.AccountPreferenceRequest;
import com.pintu.dto.AccountPreferenceVO;
import com.pintu.dto.ChangePasswordRequest;
import com.pintu.dto.ChangePhoneRequest;
import com.pintu.dto.DriverIdentityApplyRequest;
import com.pintu.dto.IdentityApplicationVO;
import com.pintu.dto.LoginResponse;
import com.pintu.dto.RoleSwitchRequest;
import com.pintu.dto.ProfileUpdateRequest;
import com.pintu.dto.CreditRecordVO;
import com.pintu.dto.UserVO;
import com.pintu.security.CurrentUser;
import com.pintu.service.UserService;
import com.pintu.service.IdentityService;
import com.pintu.service.AvatarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final IdentityService identityService;
    private final AvatarService avatarService;

    @GetMapping("/profile")
    public Result<UserVO> profile(Authentication authentication) {
        return Result.ok(userService.getProfile(CurrentUser.id(authentication)));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(
            Authentication authentication,
            @RequestBody @Valid ProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(CurrentUser.id(authentication), request));
    }

    @GetMapping("/credit-records")
    public Result<List<CreditRecordVO>> creditRecords(Authentication authentication) {
        return Result.ok(userService.listCreditRecords(CurrentUser.id(authentication)));
    }

    @GetMapping("/preferences")
    public Result<AccountPreferenceVO> preferences(Authentication authentication) {
        return Result.ok(userService.getPreferences(CurrentUser.id(authentication)));
    }

    @PutMapping("/preferences")
    public Result<AccountPreferenceVO> updatePreferences(
            Authentication authentication,
            @RequestBody @Valid AccountPreferenceRequest request) {
        return Result.ok(userService.updatePreferences(CurrentUser.id(authentication), request));
    }

    @GetMapping("/bindings")
    public Result<List<AccountBindingVO>> bindings(Authentication authentication) {
        return Result.ok(userService.listBindings(CurrentUser.id(authentication)));
    }

    @PostMapping("/bindings")
    public Result<AccountBindingVO> createBinding(
            Authentication authentication,
            @RequestBody @Valid AccountBindingRequest request) {
        return Result.ok(userService.createBinding(CurrentUser.id(authentication), request));
    }

    @DeleteMapping("/bindings/{id}")
    public Result<Void> unbind(
            Authentication authentication,
            @PathVariable String id) {
        userService.unbindAccount(CurrentUser.id(authentication), id);
        return Result.ok();
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    public Result<String> uploadAvatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        return Result.ok(avatarService.upload(CurrentUser.id(authentication), file));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            Authentication authentication,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(CurrentUser.id(authentication), request);
        return Result.ok();
    }

    @PutMapping("/phone")
    public Result<Void> changePhone(
            Authentication authentication,
            @RequestBody @Valid ChangePhoneRequest request) {
        userService.changePhone(CurrentUser.id(authentication), request);
        return Result.ok();
    }

    @PostMapping("/driver-identity")
    public Result<IdentityApplicationVO> applyForDriver(
            Authentication authentication,
            @RequestBody @Valid DriverIdentityApplyRequest request) {
        return Result.ok(identityService.applyForDriver(CurrentUser.id(authentication), request));
    }

    @GetMapping("/driver-identity")
    public Result<IdentityApplicationVO> driverIdentityStatus(Authentication authentication) {
        return Result.ok(identityService.status(CurrentUser.id(authentication)));
    }

    @PostMapping("/role")
    public Result<LoginResponse> switchRole(
            Authentication authentication,
            @RequestBody @Valid RoleSwitchRequest request) {
        return Result.ok(userService.switchRole(CurrentUser.id(authentication), request.getRole()));
    }
}
