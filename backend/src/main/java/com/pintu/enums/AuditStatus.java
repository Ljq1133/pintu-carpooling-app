package com.pintu.enums;

/**
 * 审核状态（实名审核 / 车辆审核共用）。
 */
public enum AuditStatus {

    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已驳回");

    private final String value;

    AuditStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
