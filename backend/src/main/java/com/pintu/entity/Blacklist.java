package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blacklist")
public class Blacklist {
    @TableId(type = IdType.INPUT)
    private String id;
    private String ownerId;
    private String blockedId;
    private String reason;
    private LocalDateTime createdAt;
}
