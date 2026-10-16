package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.common.Constants;
import com.guonl.common.BizException;
import com.guonl.entity.ApiSyncLog;
import com.guonl.service.SyncService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * actuator 同步
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/{projectId}")
    public ApiResult<Map<String, Object>> sync(@PathVariable Long projectId, HttpSession session) {
        return ApiResult.ok(syncService.sync(projectId, operator(session)));
    }

    @GetMapping("/log")
    public ApiResult<List<ApiSyncLog>> logs(@RequestParam Long projectId,
                                            @RequestParam(defaultValue = "20") int limit) {
        return ApiResult.ok(syncService.logs(projectId, limit));
    }

    private String operator(HttpSession session) {
        Object user = session.getAttribute(Constants.SESSION_USER);
        if (user == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return user.toString();
    }
}
