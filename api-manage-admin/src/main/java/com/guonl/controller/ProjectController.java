package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.common.PageResult;
import com.guonl.entity.ApiProject;
import com.guonl.service.ProjectService;
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
 * 项目管理
 */
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/page")
    public ApiResult<PageResult<ApiProject>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResult.ok(projectService.page(keyword, status, pageNum, Math.min(pageSize, 100)));
    }

    @GetMapping("/list")
    public ApiResult<List<ApiProject>> list() {
        return ApiResult.ok(projectService.listAll());
    }

    @GetMapping("/{projectId}")
    public ApiResult<ApiProject> get(@PathVariable Long projectId) {
        return ApiResult.ok(projectService.getById(projectId));
    }

    @PostMapping
    public ApiResult<ApiProject> create(@RequestBody ApiProject project) {
        return ApiResult.ok(projectService.create(project));
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody ApiProject project) {
        projectService.update(project);
        return ApiResult.ok();
    }

    @DeleteMapping("/{projectId}")
    public ApiResult<Void> delete(@PathVariable Long projectId) {
        projectService.delete(projectId);
        return ApiResult.ok();
    }
}
