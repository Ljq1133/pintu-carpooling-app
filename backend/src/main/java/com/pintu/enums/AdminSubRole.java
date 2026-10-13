package com.pintu.enums;

public enum AdminSubRole {
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    AUDITOR("AUDITOR", "审核员"),
    OPS("OPS", "运营"),
    CUSTOMER_SERVICE("CUSTOMER_SERVICE", "客服");

    private final String code;
    private final String label;

    AdminSubRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String code) {
        for (AdminSubRole role : values()) {
            if (role.code.equals(code)) {
                return role.label;
            }
        }
        return code;
    }
}
