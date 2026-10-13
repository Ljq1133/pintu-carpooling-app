package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal_ticket")
public class AppealTicket {

    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private String applicantId;
    private String assigneeId;
    private String reasonType;
    private String description;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
}
