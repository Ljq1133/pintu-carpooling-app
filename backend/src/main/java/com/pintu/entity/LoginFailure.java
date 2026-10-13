package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_failure")
public class LoginFailure {

    @TableId(type = IdType.INPUT)
    private String id;
    private String username;
    private String ipAddress;
    private String reason;
    private LocalDateTime createdAt;
}

