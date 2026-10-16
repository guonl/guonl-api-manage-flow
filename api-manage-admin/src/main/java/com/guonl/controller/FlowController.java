package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.common.BizException;
import com.guonl.common.Constants;
import com.guonl.common.PageResult;
import com.guonl.dto.FlowSaveRequest;
import com.guonl.entity.FlowRunStep;
import com.guonl.service.FlowService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 流程编排
 */
@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowService flowService;

    @GetMapping("/list")
    public ApiResult<PageResult<Map<String, Object>>> list(@RequestParam(required = false) Long projectId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "1") int pageNum,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> page = flowService.page(projectId, keyword, pageNum, pageSize);
        return ApiResult.ok(PageResult.of(((Number) page.get("total")).longValue(),
                castList(page.get("list"))));
    }

    @GetMapping("/{flowId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long flowId) {
        return ApiResult.ok(flowService.detail(flowId));
    }

    @PostMapping
    public ApiResult<Long> save(@RequestBody FlowSaveRequest req, HttpSession session) {
        return ApiResult.ok(flowService.save(req, operator(session)));
    }

    @PutMapping
    public ApiResult<Long> update(@RequestBody FlowSaveRequest req, HttpSession session) {
        return ApiResult.ok(flowService.save(req, operator(session)));
    }

    @DeleteMapping("/{flowId}")
    public ApiResult<Void> delete(@PathVariable Long flowId) {
        flowService.delete(flowId);
        return ApiResult.ok(null);
    }

    @PostMapping("/import")
    public ApiResult<Long> importFlow(@RequestBody String json, HttpSession session) {
        return ApiResult.ok(flowService.importFlow(json, operator(session)));
    }

    @GetMapping("/{flowId}/export")
    public ApiResult<Map<String, Object>> export(@PathVariable Long flowId) {
        return ApiResult.ok(flowService.export(flowId));
    }

    @PostMapping("/run")
    public ApiResult<Map<String, Object>> run(@RequestBody FlowSaveRequest.RunRequest req, HttpSession session) {
        return ApiResult.ok(flowService.run(req, operator(session)));
    }

    @GetMapping("/run/list")
    public ApiResult<PageResult<Map<String, Object>>> runList(@RequestParam(required = false) Long flowId,
                                                              @RequestParam(defaultValue = "1") int pageNum,
                                                              @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> page = flowService.runPage(flowId, pageNum, pageSize);
        return ApiResult.ok(PageResult.of(((Number) page.get("total")).longValue(),
                castList(page.get("list"))));
    }

    @GetMapping("/run/{runId}")
    public ApiResult<Map<String, Object>> runDetail(@PathVariable Long runId) {
        return ApiResult.ok(flowService.runDetail(runId));
    }

    @PostMapping("/run/{runId}/step/{runStepId}/rerun")
    public ApiResult<FlowRunStep> rerunStep(@PathVariable Long runId, @PathVariable Long runStepId) {
        return ApiResult.ok(flowService.rerunStep(runId, runStepId));
    }

    @SuppressWarnings("unchecked")
    private <T> java.util.List<T> castList(Object obj) {
        return obj == null ? java.util.List.of() : (java.util.List<T>) obj;
    }

    private String operator(HttpSession session) {
        Object user = session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return user.toString();
    }
}
