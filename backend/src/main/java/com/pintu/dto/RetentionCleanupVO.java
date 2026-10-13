package com.pintu.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RetentionCleanupVO {
    private Boolean dryRun;
    private String locationCutoffAt;
    private Long locationReports;
    private Long trajectoryPoints;
    private Integer policyCount;
    private List<DataRetentionPolicyVO> policies = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
}
