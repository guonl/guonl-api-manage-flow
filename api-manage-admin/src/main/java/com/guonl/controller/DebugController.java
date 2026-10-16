package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.common.BizException;
import com.guonl.common.Constants;
import com.guonl.common.PageResult;
import com.guonl.dto.DebugRequest;
import com.guonl.entity.ApiTestRecord;
import com.guonl.service.DebugService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 接口调试（类 Postman）
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final DebugService debugService;

    @PostMapping("/send")
    public ApiResult<Map<String, Object>> send(@RequestBody DebugRequest req, HttpSession session) {
        return ApiResult.ok(debugService.send(req, operator(session)));
    }

    @GetMapping("/template")
    public ApiResult<Map<String, Object>> template(@RequestParam Long urlId) {
        return ApiResult.ok(debugService.template(urlId));
    }

    @GetMapping("/records")
    public ApiResult<PageResult<ApiTestRecord>> records(@RequestParam(required = false) Long urlId,
                                                        @RequestParam(required = false) Long projectId,
                                                        @RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (Math.max(pageNum, 1) - 1) * pageSize;
        List<ApiTestRecord> list = debugService.records(urlId, projectId, offset, pageSize);
        int total = debugService.countRecords(urlId, projectId);
        return ApiResult.ok(PageResult.of(total, list));
    }

    @GetMapping("/records/recent")
    public ApiResult<List<ApiTestRecord>> recent(@RequestParam(defaultValue = "10") int limit) {
        return ApiResult.ok(debugService.recent(Math.min(limit, 50)));
    }

    @GetMapping("/record/{recordId}")
    public ApiResult<ApiTestRecord> record(@PathVariable Long recordId) {
        return ApiResult.ok(debugService.record(recordId));
    }

    private String operator(HttpSession session) {
        Object user = session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return user.toString();
    }
}
