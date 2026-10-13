package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_role_permission")
public class AdminRolePermission {

    @TableId(type = IdType.INPUT)
    private String id;
    private String subRole;
    private String permission;
    private LocalDateTime createdAt;
}
