package com.pintu.dto;

import lombok.Data;

@Data
public class DataRetentionPolicyVO {
    private String id;
    private String dataType;
    private Integer retentionDays;
    private String deleteMode;
    private String description;
    private String updatedBy;
    private String updatedAt;
}
