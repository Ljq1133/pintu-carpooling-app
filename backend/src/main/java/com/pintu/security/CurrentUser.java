package com.pintu.security;

import com.pintu.common.exception.BusinessException;
import org.springframework.security.core.Authentication;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static String id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            throw new BusinessException(1001, "请先登录");
        }
        return user.userId();
    }
}
