package com.pintu.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 正式订单状态。
 * <p>
 * 与前端约定：后端对外返回中文字符串值，B 组 adapter 已实现到四阶段的映射，
 * 因此后端无需关心前端的展示态。
 * </p>
 * <pre>
 * 待确认 -> 已确认 -> 履约中 -> 待付款 -> 已完成 -> 已归档
 *      \-------- 可取消阶段 --------/              已取消 -> 已归档
 * </pre>
 */
public enum OrderStatus {

    PENDING_CONFIRM("待确认"),
    CONFIRMED("已确认"),
    ONGOING("履约中"),
    PENDING_PAYMENT("待付款"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    ARCHIVED("已归档");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTargets().contains(target);
    }

    private Set<OrderStatus> allowedTargets() {
        return switch (this) {
            case PENDING_CONFIRM -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(ONGOING, CANCELLED);
            case ONGOING -> EnumSet.of(PENDING_PAYMENT, CANCELLED);
            case PENDING_PAYMENT -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.of(ARCHIVED);
            case ARCHIVED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态：" + value);
    }
}
