package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户视图对象，对应 B 组接口文档 §2.1 中 user 字段。
 */
@Data
public class UserVO {

    private String id;
    private String name;
    private String phone;
    private String role;
    private Integer credit;
    private String status;
    private String avatarUrl;
    private String gender;

    private Integer tripCount;
    private BigDecimal savedMoney;
    private BigDecimal carbonSaved;
}
