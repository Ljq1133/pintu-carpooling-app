package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_code")
public class VerificationCode {

    @TableId(type = IdType.INPUT)
    private String id;
    private String phoneHash;
    private String purpose;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private Integer attemptCount;
    private LocalDateTime createdAt;
}

