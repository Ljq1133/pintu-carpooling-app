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
 * 支付记录实体（MVP 阶段为模拟支付）。
 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String orderId;
    private BigDecimal amount;
    private String method;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
