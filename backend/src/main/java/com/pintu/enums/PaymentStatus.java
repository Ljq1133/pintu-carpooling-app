package com.pintu.enums;

public enum PaymentStatus {

    UNPAID("未支付"),
    PAID("已支付"),
    REFUNDED("已退款");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
