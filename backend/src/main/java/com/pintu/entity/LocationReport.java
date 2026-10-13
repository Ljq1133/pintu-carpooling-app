package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("location_report")
public class LocationReport {

    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private String userId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal accuracy;
    private BigDecimal speed;
    private BigDecimal heading;
    private String clientReportId;
    private String batchId;
    private Integer reportIntervalSeconds;
    private Integer retryCount;
    private LocalDateTime reportedAt;
    private LocalDateTime createdAt;
}
