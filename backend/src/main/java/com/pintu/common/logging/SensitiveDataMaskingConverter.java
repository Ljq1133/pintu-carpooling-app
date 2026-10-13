package com.pintu.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * 敏感字段日志脱敏转换器。
 * <p>
 * 在 logback 配置里通过 {@code %mask} 引用即可自动处理日志消息中的敏感片段，
 * 例如：JSON 中的 {@code "password":"..."}、{@code "token":"..."}、{@code "phone":"..."}。
 * </p>
 * 这是兜底措施。理想情况下业务代码不应将敏感字段写入日志，本转换器用于阻断意外泄漏。
 */
public class SensitiveDataMaskingConverter extends ClassicConverter {

    private static final Pattern[] PATTERNS = new Pattern[]{
            Pattern.compile("(?i)(\"?password\"?\\s*[:=]\\s*\"?)([^\",}\\s]+)"),
            Pattern.compile("(?i)(\"?passwd\"?\\s*[:=]\\s*\"?)([^\",}\\s]+)"),
            Pattern.compile("(?i)(\"?(access|refresh)?_?token\"?\\s*[:=]\\s*\"?)([A-Za-z0-9._\\-]{8,})"),
            Pattern.compile("(?i)(\"?authorization\"?\\s*[:=]\\s*\"?)(Bearer\\s+[A-Za-z0-9._\\-]{8,})"),
            Pattern.compile("(?i)(\"?phone\"?\\s*[:=]\\s*\"?)(\\d{11})"),
            Pattern.compile("(?i)(\"?id_?card\"?\\s*[:=]\\s*\"?)(\\d{15,18}[Xx]?)"),
            Pattern.compile("(?i)(\"?secret\"?\\s*[:=]\\s*\"?)([^\",}\\s]+)")
    };

    @Override
    public String convert(ILoggingEvent event) {
        String formatted = event.getFormattedMessage();
        if (formatted == null || formatted.isEmpty()) {
            return formatted;
        }
        String result = formatted;
        for (Pattern pattern : PATTERNS) {
            result = pattern.matcher(result).replaceAll(matchResult -> {
                int groups = matchResult.groupCount();
                String key = matchResult.group(1);
                return key + "***";
            });
        }
        return result;
    }
}
