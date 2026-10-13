package com.pintu.enums;

public enum ReviewStatus {

    UNREVIEWED("未评价"),
    REVIEWED("已评价");

    private final String value;

    ReviewStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
