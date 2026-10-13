package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("account_preference")
public class AccountPreference {
    @TableId(type = IdType.INPUT)
    private String userId;
    private Integer darkMode;
    private String fontScale;
    private String locale;
    private String protocolVersion;
    private LocalDateTime updatedAt;
}
