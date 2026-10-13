package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("data_retention_policy")
public class DataRetentionPolicy {
    @TableId(type = IdType.INPUT)
    private String id;
    private String dataType;
    private Integer retentionDays;
    private String deleteMode;
    private String description;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
