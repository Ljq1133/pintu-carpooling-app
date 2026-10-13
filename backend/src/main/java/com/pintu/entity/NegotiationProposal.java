package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("negotiation_proposal")
public class NegotiationProposal {
    @TableId(type = IdType.INPUT)
    private String id;
    private String conversationId;
    private Integer version;
    private String proposerId;
    private BigDecimal price;
    private LocalDateTime departureTime;
    private Integer passengerCount;
    private String pickupLocation;
    private BigDecimal pickupLng;
    private BigDecimal pickupLat;
    private String note;
    private LocalDateTime createdAt;
}
