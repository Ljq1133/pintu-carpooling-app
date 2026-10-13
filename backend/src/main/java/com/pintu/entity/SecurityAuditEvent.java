package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("security_audit_event")
public class SecurityAuditEvent {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String eventType;
    private String identity;
    private String ipAddress;
    private Boolean success;
    private String detail;
    private LocalDateTime createdAt;
}

