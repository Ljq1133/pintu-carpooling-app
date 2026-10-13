package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule_rollback")
public class RuleRollback {
    @TableId(type = IdType.INPUT)
    private String id;
    private String ruleId;
    private Integer fromVersion;
    private Integer toVersion;
    private String operatorId;
    private String reason;
    private LocalDateTime createdAt;
}
