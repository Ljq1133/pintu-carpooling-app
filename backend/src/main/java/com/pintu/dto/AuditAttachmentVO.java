package com.pintu.dto;

import lombok.Data;

@Data
public class AuditAttachmentVO {
    private String id;
    private String attachmentType;
    private String storageKey;
    private String contentType;
    private Long sizeBytes;
    private String createdAt;
}
