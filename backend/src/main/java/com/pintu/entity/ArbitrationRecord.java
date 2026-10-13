package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("arbitration_record")
public class ArbitrationRecord {

    @TableId(type = IdType.INPUT)
    private String id;
    private String appealId;
    private String adminId;
    private String actionType;
    private BigDecimal amount;
    private Integer creditDelta;
    private String remark;
    private String confirmedBy;
    private LocalDateTime createdAt;
}
