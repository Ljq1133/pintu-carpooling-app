package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal_evidence")
public class AppealEvidence {

    @TableId(type = IdType.INPUT)
    private String id;
    private String appealId;
    private String uploaderId;
    private String storageKey;
    private String contentType;
    private LocalDateTime createdAt;
}
