package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 匹配请求实体。
 */
@Data
@TableName("match_request")
public class MatchRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String passengerOrderId;
    private String driverTripId;
    private String passengerId;
    private String driverId;
    private String message;
    private String initiatorRole;
    private String status;
    private String rejectReason;
    private String orderId;
    private LocalDateTime handledAt;
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
