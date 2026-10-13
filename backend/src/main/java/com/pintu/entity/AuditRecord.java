package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审核记录实体（实名 / 车辆共用）。
 */
@Data
@TableName("audit_record")
public class AuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String targetType;
    private String targetId;
    private String name;
    private String plate;
    private String status;
    private String reason;
    private LocalDateTime submittedAt;
    private LocalDateTime handledAt;

    @TableLogic
    private Integer deleted;
}
