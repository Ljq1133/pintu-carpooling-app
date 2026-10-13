package com.pintu.common.trace;

import org.slf4j.MDC;

/**
 * 在异常响应、领域事件或异步任务中读取当前 TraceId 的便捷入口。
 */
public final class TraceContext {

    private TraceContext() {
    }

    public static String currentTraceId() {
        String value = MDC.get(TraceIdFilter.MDC_KEY);
        return value == null ? "" : value;
    }
}
