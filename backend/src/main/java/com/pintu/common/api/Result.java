package com.pintu.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pintu.common.trace.TraceContext;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构。
 * <pre>
 *   code = 0  表示成功
 *   code != 0 表示业务异常，msg 描述原因
 * </pre>
 * traceId 字段在错误响应中返回，便于排查问题；成功响应可省略。
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String traceId;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMsg("ok");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setTraceId(TraceContext.currentTraceId());
        return result;
    }
}
