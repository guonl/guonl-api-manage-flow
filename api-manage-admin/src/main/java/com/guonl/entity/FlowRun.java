package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程运行记录
 */
@Data
public class FlowRun {

    private Long runId;

    private Long flowId;

    /** 运行时流程名快照 */
    private String flowNameSnapshot;

    /** MANUAL / DEBUG / RERUN / AI */
    private String triggerType;

    /** 初始变量 JSON */
    private String initVars;

    /** 终态上下文 JSON（成功值/提取值） */
    private String contextSnapshot;

    /** RUNNING / SUCCESS / FAILED */
    private String runStatus;

    private Integer totalCostMs;

    private String errorMsg;

    private String operator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
