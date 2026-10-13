package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("safety_alert")
public class SafetyAlert {

    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private String userId;
    private String alertType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String details;
    private String status;
    private String handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;
}
