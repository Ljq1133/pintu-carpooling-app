package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule_config")
public class RuleConfig {
    @TableId(type = IdType.INPUT)
    private String id;
    private String ruleType;
    private String code;
    private String name;
    private Integer activeVersion;
    private String status;
    private Integer grayPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
