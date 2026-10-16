package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.entity.ApiGroup;
import com.guonl.service.GroupService;
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
 * 接口分组
 */
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/list")
    public ApiResult<List<ApiGroup>> list(@RequestParam Long projectId) {
        return ApiResult.ok(groupService.listByProject(projectId));
    }

    @GetMapping("/{groupId}")
    public ApiResult<ApiGroup> get(@PathVariable Long groupId) {
        return ApiResult.ok(groupService.getById(groupId));
    }

    @PostMapping
    public ApiResult<ApiGroup> create(@RequestBody ApiGroup group) {
        return ApiResult.ok(groupService.create(group));
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody ApiGroup group) {
        groupService.update(group);
        return ApiResult.ok();
    }

    @DeleteMapping("/{groupId}")
    public ApiResult<Void> delete(@PathVariable Long groupId) {
        groupService.delete(groupId);
        return ApiResult.ok();
    }
}
