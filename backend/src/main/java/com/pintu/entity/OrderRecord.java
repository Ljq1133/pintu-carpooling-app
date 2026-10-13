package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 正式订单实体。双方确认后形成。
 */
@Data
@TableName("order_record")
public class OrderRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String passengerOrderId;
    private String driverTripId;
    private String matchRequestId;
    private String passengerId;
    private String driverId;
    private String startLocation;
    private String endLocation;
    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private LocalDateTime departureTime;
    private LocalDateTime actualDeparture;
    private LocalDateTime actualArrival;
    private Integer passengerCount;
    private BigDecimal price;
    private String status;
    private String paymentStatus;
    private String reviewStatus;
    private String cancelReason;
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
