package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_binding")
public class AccountBinding {
    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String bindingType;
    private String provider;
    private String accountLabel;
    private String accountValue;
    private String maskedValue;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
