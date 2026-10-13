package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_history")
public class AuditHistory {

    @TableId(type = IdType.INPUT)
    private String id;
    private String auditRecordId;
    private String adminId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private LocalDateTime createdAt;
}

