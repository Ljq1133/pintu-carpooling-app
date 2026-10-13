package com.pintu.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOpsStatisticsVO {
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long activeOrders;
    private Long safetyAlertCount;
    private Long openAppealCount;
    private BigDecimal conversionRate;
    private BigDecimal completionRate;
    private BigDecimal cancellationRate;
    private BigDecimal averageMatchMinutes;
    private BigDecimal abnormalRate;
}
