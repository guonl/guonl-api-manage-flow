package com.guonl.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResult<Void> handleNotFound(NoResourceFoundException e) {
        return ApiResult.fail(404, "资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ApiResult.fail(500, "系统异常：" + e.getMessage());
    }
}
