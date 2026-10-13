package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trajectory_point")
public class TrajectoryPoint {

    @TableId(type = IdType.INPUT)
    private String id;
    private String orderId;
    private Integer sequenceNo;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDateTime recordedAt;
}
