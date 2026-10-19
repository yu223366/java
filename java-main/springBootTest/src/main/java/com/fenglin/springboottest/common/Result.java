package com.fenglin.springboottest.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。
 * 前端约定：{ code: 0, message: 'ok', data: {...} }，code !== 0 视为失败。
 */
@Data
public class Result<T> implements Serializable {

    /** 0 = 成功；非 0 = 失败 */
    private int code;
    /** 提示文案 */
    private String message;
    /** 业务数据 */
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
