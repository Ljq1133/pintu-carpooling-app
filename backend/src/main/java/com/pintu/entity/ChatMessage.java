package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.INPUT)
    private String id;
    private String conversationId;
    private String senderId;
    private String messageType;
    private String content;
    private String attachmentKey;
    private String clientMessageId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
