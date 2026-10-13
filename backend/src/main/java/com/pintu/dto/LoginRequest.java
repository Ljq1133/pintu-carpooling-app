package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * 登录请求。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 角色：passenger / driver / admin。MVP 阶段允许为空，后端按用户表的 role 字段返回。 */
    private String role;

    @JsonIgnore
    private String ipAddress;
}
