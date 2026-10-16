package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程步骤
 */
@Data
public class FlowStep {

    private Long stepId;

    private Long flowId;

    /** 步骤名 */
    private String stepName;

    /** 顺序 */
    private Integer sortOrder;

    /** HTTP（预留 DELAY/SCRIPT） */
    private String stepType;

    /** 关联接口（可空） */
    private Long urlId;

    private String requestMethod;

    /** 支持 {{var}} 占位 */
    private String requestUrl;

    /** JSON 模板 */
    private String requestHeaders;

    /** JSON 模板 */
    private String requestQuery;

    /** none/json/form-data/x-www-form-urlencoded */
    private String bodyType;

    /** 体模板 */
    private String requestBody;

    /** JSON 数组：[{varName,jsonPath,desc}] */
    private String extractRules;

    /** JSON 数组：[{type,target,expect,desc}] */
    private String assertions;

    /** TERMINATE / CONTINUE */
    private String onFailure;

    /** 执行前等待毫秒 */
    private Integer delayMs;

    /** 1启用 0禁用 */
    private String enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String delFlag;
}
