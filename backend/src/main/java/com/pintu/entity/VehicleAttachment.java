package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vehicle_attachment")
public class VehicleAttachment {

    @TableId(type = IdType.INPUT)
    private String id;
    private String vehicleId;
    private String attachmentType;
    private String storageKey;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}

