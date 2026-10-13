package com.pintu.dto;

import lombok.Data;

@Data
public class AttachmentUploadVO {
    private String attachmentKey;
    private String fileName;
    private String contentType;
    private long size;
}
