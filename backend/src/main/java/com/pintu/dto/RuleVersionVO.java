package com.pintu.dto;

import lombok.Data;

@Data
public class RuleVersionVO {
    private String id;
    private Integer version;
    private String content;
    private String status;
    private String createdBy;
    private String publishedAt;
    private String createdAt;
}
