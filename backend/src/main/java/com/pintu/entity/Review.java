package com.pintu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单评价实体。
 */
@Data
@TableName("review")
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    private String orderId;
    private String passengerId;
    private String driverId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
