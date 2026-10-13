package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ban_record")
public class BanRecord {
    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String banType;
    private String reason;
    private String operatorId;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime releasedAt;
}
