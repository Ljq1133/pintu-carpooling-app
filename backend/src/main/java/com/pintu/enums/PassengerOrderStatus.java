package com.pintu.enums;

/**
 * 乘客需求的聚合视图状态。
 *
 * <p>待匹配和协商中由 passenger_order 承载；确认后与正式订单状态同步，
 * 供前端以一条连续生命周期展示。</p>
 */
public enum PassengerOrderStatus {

    MATCHING("待匹配"),
    NEGOTIATING("协商中"),
    CONFIRMED("已确认"),
    ONGOING("履约中"),
    PENDING_PAYMENT("待付款"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    ARCHIVED("已归档");

    private final String value;

    PassengerOrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PassengerOrderStatus fromValue(String value) {
        for (PassengerOrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知乘客需求状态：" + value);
    }
}
