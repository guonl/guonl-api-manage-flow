package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.entity.ApiEnv;
import com.guonl.service.EnvService;
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

import java.util.List;

/**
 * 环境管理
 */
@RestController
@RequestMapping("/api/env")
@RequiredArgsConstructor
public class EnvController {

    private final EnvService envService;

    @GetMapping("/list")
    public ApiResult<List<ApiEnv>> list(@RequestParam Long projectId) {
        return ApiResult.ok(envService.listByProject(projectId));
    }

    @PostMapping
    public ApiResult<ApiEnv> create(@RequestBody ApiEnv env) {
        return ApiResult.ok(envService.create(env));
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody ApiEnv env) {
        envService.update(env);
        return ApiResult.ok();
    }

    @DeleteMapping("/{envId}")
    public ApiResult<Void> delete(@PathVariable Long envId) {
        envService.delete(envId);
        return ApiResult.ok();
    }
}
