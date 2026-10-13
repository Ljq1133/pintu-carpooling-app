package com.pintu.enums;

public enum AdminPermission {
    AUDIT_REVIEW("AUDIT_REVIEW", "\u5ba1\u6838\u5904\u7406"),
    ORDER_VIEW("ORDER_VIEW", "\u8ba2\u5355\u770b\u677f"),
    CREDIT_MANAGE("CREDIT_MANAGE", "\u4fe1\u7528\u7ba1\u7406"),
    APPEAL_MANAGE("APPEAL_MANAGE", "\u7533\u8bc9\u5de5\u5355"),
    ARBITRATION_MANAGE("ARBITRATION_MANAGE", "\u7533\u8bc9\u4ef2\u88c1"),
    SAFETY_MONITOR("SAFETY_MONITOR", "\u5b89\u5168\u76d1\u63a7"),
    RULE_MANAGE("RULE_MANAGE", "\u89c4\u5219\u914d\u7f6e"),
    SUPPORT_MANAGE("SUPPORT_MANAGE", "\u5ba2\u670d\u4f1a\u8bdd"),
    USER_RESTRICT("USER_RESTRICT", "\u7528\u6237\u9650\u5236");

    private final String code;
    private final String label;

    AdminPermission(String code, String label) {
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
        for (AdminPermission permission : values()) {
            if (permission.code.equals(code)) {
                return permission.label;
            }
        }
        return code;
    }
}
