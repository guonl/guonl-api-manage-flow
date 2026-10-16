package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.FlowInfoDao;
import com.guonl.dao.FlowRunDao;
import com.guonl.dao.FlowStepDao;
import com.guonl.dto.FlowSaveRequest;
import com.guonl.entity.FlowInfo;
import com.guonl.entity.FlowRun;
import com.guonl.entity.FlowRunStep;
import com.guonl.entity.FlowStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程编排：定义管理 + 导入导出 + 运行入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final FlowInfoDao flowInfoDao;
    private final FlowStepDao flowStepDao;
    private final FlowRunDao flowRunDao;
    private final FlowEngine flowEngine;

    public Map<String, Object> page(Long projectId, String keyword, int pageNum, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(pageNum, 1) - 1) * size;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", flowInfoDao.countPage(projectId, keyword));
        result.put("list", flowInfoDao.selectPage(projectId, keyword, offset, size));
        return result;
    }

    /** 详情（含步骤列表） */
    public Map<String, Object> detail(Long flowId) {
        FlowInfo flow = flowInfoDao.selectById(flowId);
        if (flow == null) {
            throw new BizException(404, "流程不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flow", flow);
        result.put("steps", flowStepDao.listByFlowId(flowId));
        return result;
    }

    /** 保存（新增或更新），步骤随主单整体替换 */
    @Transactional(rollbackFor = Exception.class)
    public Long save(FlowSaveRequest req, String operator) {
        if (!StringUtils.hasText(req.getFlowName())) {
            throw new BizException(400, "流程名称不能为空");
        }
        if (req.getProjectId() == null) {
            throw new BizException(400, "归属项目不能为空");
        }
        FlowInfo flow = new FlowInfo();
        flow.setFlowName(req.getFlowName().trim());
        flow.setFlowDesc(req.getFlowDesc());
        flow.setProjectId(req.getProjectId());
        flow.setFlowStatus(StringUtils.hasText(req.getFlowStatus()) ? req.getFlowStatus() : "1");
        if (req.getFlowId() == null) {
            flowInfoDao.insert(flow);
        } else {
            flow.setFlowId(req.getFlowId());
            if (flowInfoDao.selectById(flow.getFlowId()) == null) {
                throw new BizException(404, "流程不存在");
            }
            flowInfoDao.update(flow);
        }

        // 步骤整体替换
        flowStepDao.deleteByFlowId(flow.getFlowId());
        List<FlowStep> steps = req.getSteps();
        if (steps != null) {
            int sort = 0;
            for (FlowStep step : steps) {
                if (step == null || !StringUtils.hasText(step.getStepName())) {
                    continue;
                }
                normalizeStep(step);
                step.setStepId(null);
                step.setFlowId(flow.getFlowId());
                step.setSortOrder(++sort);
                flowStepDao.insert(step);
            }
        }
        return flow.getFlowId();
    }

    public void delete(Long flowId) {
        if (flowInfoDao.selectById(flowId) == null) {
            throw new BizException(404, "流程不存在");
        }
        flowInfoDao.deleteById(flowId);
    }

    /** 导入流程 JSON */
    @Transactional(rollbackFor = Exception.class)
    public Long importFlow(String json, String operator) {
        if (!StringUtils.hasText(json)) {
            throw new BizException(400, "导入内容不能为空");
        }
        FlowSaveRequest req;
        try {
            req = JSON_MAPPER.readValue(json, FlowSaveRequest.class);
        } catch (Exception e) {
            throw new BizException(400, "JSON 格式不正确：" + e.getMessage());
        }
        return save(req, operator);
    }

    /** 导出流程 JSON（含步骤） */
    public Map<String, Object> export(Long flowId) {
        Map<String, Object> detail = detail(flowId);
        FlowInfo flow = (FlowInfo) detail.get("flow");
        @SuppressWarnings("unchecked")
        List<FlowStep> steps = (List<FlowStep>) detail.get("steps");
        Map<String, Object> exportData = new LinkedHashMap<>();
        exportData.put("flowName", flow.getFlowName());
        exportData.put("projectId", flow.getProjectId());
        exportData.put("flowDesc", flow.getFlowDesc());
        exportData.put("flowStatus", flow.getFlowStatus());
        exportData.put("steps", steps);
        return exportData;
    }

    /** 立即运行（同步） */
    public Map<String, Object> run(FlowSaveRequest.RunRequest req, String operator) {
        if (req.getFlowId() == null) {
            throw new BizException(400, "flowId 不能为空");
        }
        String mode = StringUtils.hasText(req.getMode()) ? req.getMode() : "NORMAL";
        FlowRun run = flowEngine.run(req.getFlowId(), req.getEnvId(), req.getInitVars(), mode, operator);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runId", run.getRunId());
        result.put("runStatus", run.getRunStatus());
        result.put("totalCostMs", run.getTotalCostMs());
        result.put("errorMsg", run.getErrorMsg());
        return result;
    }

    public Map<String, Object> runDetail(Long runId) {
        FlowRun run = flowRunDao.selectById(runId);
        if (run == null) {
            throw new BizException(404, "运行记录不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("run", run);
        result.put("steps", flowRunDao.selectStepsByRunId(runId));
        return result;
    }

    public Map<String, Object> runPage(Long flowId, int pageNum, int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(pageNum, 1) - 1) * size;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", flowRunDao.countPage(flowId));
        result.put("list", flowRunDao.selectPage(flowId, offset, size));
        return result;
    }

    /** 单步重跑 */
    public FlowRunStep rerunStep(Long runId, Long runStepId) {
        FlowRun run = flowRunDao.selectById(runId);
        if (run == null) {
            throw new BizException(404, "运行记录不存在");
        }
        FlowRunStep step = flowRunDao.selectStepById(runStepId);
        if (step == null || !run.getRunId().equals(step.getRunId())) {
            throw new BizException(404, "运行明细不存在");
        }
        return flowEngine.rerunStep(runStepId);
    }

    /** 步骤默认值兜底 */
    private void normalizeStep(FlowStep step) {
        if (!StringUtils.hasText(step.getStepType())) {
            step.setStepType("HTTP");
        }
        if (!StringUtils.hasText(step.getRequestMethod())) {
            step.setRequestMethod("GET");
        }
        if (!StringUtils.hasText(step.getBodyType())) {
            step.setBodyType("none");
        }
        if (!StringUtils.hasText(step.getOnFailure())) {
            step.setOnFailure("TERMINATE");
        }
        if (!StringUtils.hasText(step.getEnabled())) {
            step.setEnabled("1");
        }
        if (step.getDelayMs() == null) {
            step.setDelayMs(0);
        }
        if (step.getSortOrder() == null) {
            step.setSortOrder(0);
        }
    }
}
