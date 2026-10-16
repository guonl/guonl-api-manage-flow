package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义
 */
@Data
public class FlowInfo {

    private Long flowId;

    /** 流程名称 */
    private String flowName;

    /** 描述 */
    private String flowDesc;

    /** 归属项目 */
    private Long projectId;

    /** 1启用 0停用 */
    private String flowStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String delFlag;
}
