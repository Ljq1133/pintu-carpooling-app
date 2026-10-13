package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emergency_contact")
public class EmergencyContact {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String name;
    private String phoneCipher;
    private String phoneMasked;
    private String relationship;
    private Integer isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
