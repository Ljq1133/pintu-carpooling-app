package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体：乘客 / 司机 / 管理员统一表。
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String username;
    private String password;
    private String name;
    private String phone;
    private String phoneHash;
    private String avatarUrl;
    private String gender;
    private String role;
    private Integer credit;
    private String status;
    private Integer tokenVersion;
    private Integer tripCount;
    private BigDecimal savedMoney;
    private BigDecimal carbonSaved;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
