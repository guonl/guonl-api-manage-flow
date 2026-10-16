package com.guonl.dto;

import com.guonl.entity.FlowStep;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 流程保存请求（步骤随主单整体提交）
 */
@Data
public class FlowSaveRequest {

    private Long flowId;

    private Long projectId;

    private String flowName;

    private String flowDesc;

    /** 1启用 0停用 */
    private String flowStatus;

    private List<FlowStep> steps;

    /**
     * 运行请求
     */
    @Data
    public static class RunRequest {
        private Long flowId;
        /** 环境为空则取项目默认环境 */
        private Long envId;
        /** 初始变量 */
        private Map<String, Object> initVars;
        /** DEBUG=失败即停 NORMAL=按步骤 on_failure 策略 */
        private String mode;
    }
}
