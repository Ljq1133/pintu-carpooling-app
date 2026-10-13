package com.pintu.dto;

import lombok.Data;

import java.util.List;

@Data
public class RuleConfigVO {
    private String id;
    private String ruleType;
    private String code;
    private String name;
    private Integer activeVersion;
    private String status;
    private Integer grayPercent;
    private String createdAt;
    private String updatedAt;
    private List<RuleVersionVO> versions;
}
