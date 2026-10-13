package com.pintu.enums;

/**
 * 用户账号状态，与信用分阈值联动。
 * <pre>
 *   credit >= 80      -> 正常
 *   60 <= credit < 80 -> 观察
 *   credit < 60       -> 限制发单 / 限制接单（按角色）
 *   人工封禁           -> 已封禁
 * </pre>
 */
public enum UserStatus {

    NORMAL("正常"),
    OBSERVING("观察"),
    POST_LIMITED("限制发单"),
    ACCEPT_LIMITED("限制接单"),
    BANNED("已封禁");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
