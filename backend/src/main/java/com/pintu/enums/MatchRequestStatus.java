package com.pintu.enums;

/**
 * 匹配请求状态。
 * <pre>
 * 待处理 -> 已同意
 *        -> 已拒绝
 *        -> 已失效
 * </pre>
 */
public enum MatchRequestStatus {

    PENDING("待处理"),
    ACCEPTED("已同意"),
    REJECTED("已拒绝"),
    EXPIRED("已失效"),
    WITHDRAWN("已撤回");

    private final String value;

    MatchRequestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
