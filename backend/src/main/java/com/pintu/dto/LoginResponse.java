package com.pintu.dto;

import lombok.Data;

/**
 * 登录响应。
 * 登录和刷新成功后的令牌响应。
 */
@Data
public class LoginResponse {

    private String token;
    private String refreshToken;
    private long expiresIn;
    private UserVO user;
}
