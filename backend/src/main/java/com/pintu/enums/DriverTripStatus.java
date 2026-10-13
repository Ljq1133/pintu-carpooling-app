package com.pintu.enums;

public enum DriverTripStatus {

    PUBLISHED("已发布"),
    FULL("已满座"),
    ONGOING("履约中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String value;

    DriverTripStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
