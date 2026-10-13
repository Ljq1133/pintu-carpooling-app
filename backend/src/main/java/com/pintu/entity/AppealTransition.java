package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal_transition")
public class AppealTransition {

    @TableId(type = IdType.INPUT)
    private String id;
    private String appealId;
    private String operatorId;
    private String fromStatus;
    private String toStatus;
    private String comment;
    private LocalDateTime createdAt;
}
