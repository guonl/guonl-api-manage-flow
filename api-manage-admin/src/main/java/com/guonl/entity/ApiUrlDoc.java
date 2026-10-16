package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口文档扩展表（与 api_url 1:1）
 */
@Data
public class ApiUrlDoc {

    private Long urlId;

    /** Request Headers：Map-JSON格式字符串 */
    private String requestHeaders;

    /** Query String Parameters：VO-JSON格式字符串 */
    private String queryParams;

    private String successRespExample;

    private String failRespExample;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
