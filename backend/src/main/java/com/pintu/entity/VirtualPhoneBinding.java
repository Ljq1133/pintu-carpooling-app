package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("virtual_phone_binding")
public class VirtualPhoneBinding {

    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private String passengerId;
    private String driverId;
    private String virtualNumber;
    private String providerBindingId;
    private String status;
    private LocalDateTime boundAt;
    private LocalDateTime unboundAt;
    private String unbindReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
