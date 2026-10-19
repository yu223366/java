package com.fenglin.springboottest.common;

/**
 * 业务异常：携带错误码与文案，由全局异常处理器统一转换为 Result。
 */
public class BizException extends RuntimeException {

    private int code;

    public BizException(String message) {
        super(message);
        this.code = 1;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
