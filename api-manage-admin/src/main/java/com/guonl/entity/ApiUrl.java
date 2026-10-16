package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口主表
 */
@Data
public class ApiUrl {

    private Long urlId;

    private Long groupId;

    /** 冗余项目id（树查询加速） */
    private Long projectId;

    private String requestUrl;

    private String methodName;

    private String methodType;

    /** 请求体类型 form/json/none */
    private String methodContentType;

    /** 请求参数 JSON {name:{type,desc}} */
    private String requestParamMap;

    private String returnParamClass;

    private String returnParamMap;

    /** 0-禁用 1-启用 2-已下线 */
    private Integer urlStatus;

    private String urlLabel;

    private String methodDesc;

    /** 来源:1-同步 2-手工 3-调试另存 */
    private Integer sourceType;

    private LocalDateTime lastSyncTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
