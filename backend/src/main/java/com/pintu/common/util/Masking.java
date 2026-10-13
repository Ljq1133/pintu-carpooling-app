package com.pintu.common.util;

public final class Masking {

    private Masking() {
    }

    public static String phone(String value) {
        if (value == null || value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    public static String plate(String value) {
        if (value == null || value.length() < 3) {
            return value;
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 1);
    }

    public static String name(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1) + "**";
    }
}

