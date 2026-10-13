package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private String bizId;
    private LocalDateTime readAt;
    private String sendStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
}
