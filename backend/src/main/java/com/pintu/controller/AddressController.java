package com.pintu.controller;

import com.pintu.common.api.Result;
import com.pintu.dto.UserAddressSaveRequest;
import com.pintu.entity.UserAddress;
import com.pintu.security.CurrentUser;
import com.pintu.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @GetMapping
    public Result<List<UserAddress>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "HISTORY") String type) {
        return Result.ok(addressService.list(CurrentUser.id(authentication), type));
    }

    @PostMapping("/favorites")
    public Result<UserAddress> saveFavorite(
            Authentication authentication,
            @RequestBody @Valid UserAddressSaveRequest request) {
        return Result.ok(addressService.saveFavorite(CurrentUser.id(authentication), request));
    }

    @DeleteMapping("/favorites/{addressId}")
    public Result<Void> deleteFavorite(
            Authentication authentication,
            @PathVariable String addressId) {
        addressService.deleteFavorite(CurrentUser.id(authentication), addressId);
        return Result.ok();
    }
}
