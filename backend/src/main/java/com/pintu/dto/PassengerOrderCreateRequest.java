package com.pintu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建乘客拼车需求请求体。
 * 对应 B 组接口文档 §3.1。
 */
@Data
public class PassengerOrderCreateRequest {

    @NotBlank(message = "出发地不能为空")
    private String start;

    @NotBlank(message = "目的地不能为空")
    private String end;

    @DecimalMin(value = "-180.0", message = "起点经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "起点经度不能大于 180")
    private BigDecimal startLng;

    @DecimalMin(value = "-90.0", message = "起点纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "起点纬度不能大于 90")
    private BigDecimal startLat;

    @DecimalMin(value = "-180.0", message = "终点经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "终点经度不能大于 180")
    private BigDecimal endLng;

    @DecimalMin(value = "-90.0", message = "终点纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "终点纬度不能大于 90")
    private BigDecimal endLat;

    @NotNull(message = "出发时间不能为空")
    private LocalDateTime departureTime;

    @NotNull
    @Positive(message = "乘车人数必须大于 0")
    private Integer passengerCount;

    private String note;

    /** 演示账号场景下指定乘客 ID。生产环境应从登录态获取。 */
    private String passengerId;
}
