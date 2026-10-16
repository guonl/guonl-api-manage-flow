package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程步骤运行明细
 */
@Data
public class FlowRunStep {

    private Long runStepId;

    private Long runId;

    private Long stepId;

    /** 步骤名快照 */
    private String stepName;

    private Integer sortOrder;

    /** 渲染后请求 JSON（method/url/headers/query/body） */
    private String requestSnapshot;

    private Integer responseStatus;

    /** 响应体（按 max-body-log-size 截断） */
    private String responseBody;

    /** 提取结果 JSON：{varName: value} */
    private String extractResult;

    /** 断言结果 JSON：[{type,target,expect,actual,pass}] */
    private String assertResult;

    /** SUCCESS / FAILED / SKIPPED */
    private String stepStatus;

    private Integer costMs;

    private String errorMsg;

    private LocalDateTime createTime;
}
