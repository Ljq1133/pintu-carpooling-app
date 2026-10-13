package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule_version")
public class RuleVersion {
    @TableId(type = IdType.INPUT)
    private String id;
    private String ruleId;
    private Integer version;
    private String content;
    private String status;
    private String createdBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
