package com.pintu.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 * 用于在 Service 层抛出可预期的错误（状态非法、参数缺失、数据不存在等）。
 * 由 {@link com.pintu.common.exception.GlobalExceptionHandler} 统一捕获并返回 {@link com.pintu.common.api.Result}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String msg) {
        super(msg);
        this.code = 1001;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
