package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("identity_application")
public class IdentityApplication {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String realNameCipher;
    private String idCardCipher;
    private String idCardMasked;
    private String livenessResult;
    private String status;
    private String rejectReason;
    private LocalDateTime submittedAt;
    private LocalDateTime handledAt;

    @TableLogic
    private Integer deleted;
}

