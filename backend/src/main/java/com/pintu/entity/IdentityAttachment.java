package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("identity_attachment")
public class IdentityAttachment {

    @TableId(type = IdType.INPUT)
    private String id;
    private String applicationId;
    private String attachmentType;
    private String storageKey;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}

