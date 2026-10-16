package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.entity.ApiUrl;
import com.guonl.entity.ApiUrlDoc;
import com.guonl.dao.ApiUrlDocDao;
import com.guonl.service.ApiUrlService;
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
import java.util.Map;

/**
 * 接口管理
 */
@RestController
@RequestMapping("/api/api")
@RequiredArgsConstructor
public class ApiUrlController {

    private final ApiUrlService apiUrlService;
    private final ApiUrlDocDao apiUrlDocDao;

    /** 项目接口树：分组 → 接口 */
    @GetMapping("/tree")
    public ApiResult<List<Map<String, Object>>> tree(@RequestParam Long projectId) {
        return ApiResult.ok(apiUrlService.tree(projectId));
    }

    @GetMapping("/search")
    public ApiResult<List<ApiUrl>> search(@RequestParam Long projectId,
                                          @RequestParam(required = false) String keyword) {
        return ApiResult.ok(apiUrlService.search(projectId, keyword));
    }

    @GetMapping("/{urlId}")
    public ApiResult<Map<String, Object>> get(@PathVariable Long urlId) {
        ApiUrl url = apiUrlService.getById(urlId);
        ApiUrlDoc doc = apiUrlDocDao.selectByUrlId(urlId);
        return ApiResult.ok(Map.of("api", url, "doc", doc == null ? new ApiUrlDoc() : doc));
    }

    @PostMapping
    public ApiResult<ApiUrl> create(@RequestBody ApiUrl url) {
        return ApiResult.ok(apiUrlService.create(url));
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody ApiUrl url) {
        apiUrlService.update(url);
        return ApiResult.ok();
    }

    @PutMapping("/{urlId}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long urlId, @RequestParam Integer status) {
        apiUrlService.updateStatus(urlId, status);
        return ApiResult.ok();
    }

    @DeleteMapping("/{urlId}")
    public ApiResult<Void> delete(@PathVariable Long urlId) {
        apiUrlService.delete(urlId);
        return ApiResult.ok();
    }

    /** 接口文档扩展（1:1 upsert） */
    @PutMapping("/{urlId}/doc")
    public ApiResult<Void> saveDoc(@PathVariable Long urlId, @RequestBody ApiUrlDoc doc) {
        apiUrlService.getById(urlId);
        doc.setUrlId(urlId);
        apiUrlDocDao.upsert(doc);
        return ApiResult.ok();
    }
}
