package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_message")
public class SupportMessage {
    @TableId(type = IdType.INPUT)
    private String id;
    private String sessionId;
    private String senderId;
    private String senderRole;
    private String messageType;
    private String content;
    private String attachmentKey;
    private String clientMessageId;
    private LocalDateTime createdAt;
}
