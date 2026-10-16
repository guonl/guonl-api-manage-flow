package com.guonl.common;

import lombok.Data;

/**
 * 统一响应结构 {code, msg, data}，code=0 表示成功
 */
@Data
public class ApiResult<T> {

    public static final int SUCCESS = 0;

    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = new ApiResult<>();
        r.code = SUCCESS;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> fail(int code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> ApiResult<T> fail(String msg) {
        return fail(500, msg);
    }

    public boolean isSuccess() {
        return code == SUCCESS;
    }
}
