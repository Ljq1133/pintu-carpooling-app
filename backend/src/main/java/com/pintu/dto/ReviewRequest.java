package com.pintu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交订单评价请求体。对应 B 组接口文档 §5.3。
 */
@Data
public class ReviewRequest {

    @NotNull
    @Min(value = 1, message = "评分至少 1 分")
    @Max(value = 5, message = "评分最多 5 分")
    private Integer rating;

    private String comment;
}
