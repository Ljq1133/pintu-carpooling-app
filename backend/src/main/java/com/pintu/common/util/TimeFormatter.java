package com.pintu.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式工具。
 * 前端约定使用 {@code yyyy-MM-dd HH:mm} 字符串展示，避免 ISO 格式中的 {@code T} 分隔符。
 */
public final class TimeFormatter {

    public static final DateTimeFormatter PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private TimeFormatter() {
    }

    public static String now() {
        return format(nowDateTime());
    }

    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(PATTERN);
    }
}
