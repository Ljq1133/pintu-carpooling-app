package com.pintu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须为 6 到 64 位")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过 64 个字符")
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Code must contain 6 digits")
    private String code;
}
