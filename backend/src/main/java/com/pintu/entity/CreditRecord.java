package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 信用分变动记录实体。
 */
@Data
@TableName("credit_record")
public class CreditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private Integer delta;
    private String reason;
    private String operatorId;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
