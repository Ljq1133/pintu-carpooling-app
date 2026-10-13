package com.pintu.enums;

/**
 * 用户角色。
 */
public enum UserRole {

    PASSENGER("passenger"),
    DRIVER("driver"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
