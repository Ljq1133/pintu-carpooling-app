package com.pintu.dto;

import lombok.Data;

/**
 * 评价响应视图。对应 B 组接口文档 §5.3。
 */
@Data
public class ReviewVO {

    private String id;
    private String orderId;
    private Integer rating;
    private String comment;
    private String status;
    private String orderStatus;
}
