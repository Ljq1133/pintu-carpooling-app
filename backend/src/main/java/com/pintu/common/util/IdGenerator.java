package com.pintu.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务 ID 生成器。
 * 沿用前端约定的 ID 前缀，保证演示截图与日志可读：
 * <pre>
 *   passenger-... / driver-... / po-... / trip-... / req-... / order-... / pay-... / review-...
 * </pre>
 * 当前实现：时间戳 + 进程内自增序列。MVP 阶段足够使用，演示数据量不大。
 */
public final class IdGenerator {

    public static final String PREFIX_PASSENGER_ORDER = "po";
    public static final String PREFIX_DRIVER_TRIP = "trip";
    public static final String PREFIX_MATCH_REQUEST = "req";
    public static final String PREFIX_ORDER = "order";
    public static final String PREFIX_PAYMENT = "pay";
    public static final String PREFIX_REVIEW = "review";
    public static final String PREFIX_VEHICLE = "veh";
    public static final String PREFIX_AUDIT = "audit";
    public static final String PREFIX_CREDIT = "credit";

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private IdGenerator() {
    }

    /**
     * 生成业务 ID。
     *
     * @param prefix 前缀，例如 {@link #PREFIX_PASSENGER_ORDER}
     * @return 形如 po-1718260000000-12 的字符串
     */
    public static String next(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + SEQUENCE.incrementAndGet();
    }
}
