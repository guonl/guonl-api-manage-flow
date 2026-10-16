package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调试调用历史
 */
@Data
public class ApiTestRecord {

    private Long recordId;

    private Long projectId;

    private Long envId;

    /** 关联接口（可空=自由调试） */
    private Long urlId;

    private String requestMethod;

    /** 实际完整 URL */
    private String requestUrl;

    /** JSON */
    private String requestHeaders;

    /** JSON */
    private String requestQuery;

    private String requestBody;

    /** none/json/form-data/x-www-form-urlencoded */
    private String bodyType;

    private Integer responseStatus;

    /** JSON */
    private String responseHeaders;

    /** 按 max-body-log-size 截断 */
    private String responseBody;

    private Integer costMs;

    /** 1成功(2xx) 0失败 */
    private Integer success;

    private String errorMsg;

    private String operator;

    private LocalDateTime createTime;
}
