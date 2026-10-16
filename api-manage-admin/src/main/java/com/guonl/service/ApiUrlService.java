package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiGroupDao;
import com.guonl.dao.ApiUrlDao;
import com.guonl.dao.ApiUrlDocDao;
import com.guonl.entity.ApiGroup;
import com.guonl.entity.ApiUrl;
import com.guonl.entity.ApiUrlDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiUrlService {

    private final ApiUrlDao urlDao;
    private final ApiGroupDao groupDao;
    private final ApiUrlDocDao docDao;

    /* ---------- 树结构 ---------- */

    public List<Map<String, Object>> tree(Long projectId) {
        List<ApiGroup> groups = groupDao.selectByProject(projectId);
        List<ApiUrl> urls = urlDao.selectByProject(projectId);

        Map<Long, List<ApiUrl>> urlsByGroup = new LinkedHashMap<>();
        for (ApiUrl u : urls) {
            urlsByGroup.computeIfAbsent(u.getGroupId(), k -> new ArrayList<>()).add(u);
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        for (ApiGroup g : groups) {
            Map<String, Object> gNode = new LinkedHashMap<>();
            gNode.put("id", "g" + g.getGroupId());
            gNode.put("groupId", g.getGroupId());
            gNode.put("label", g.getControllerShortName());
            gNode.put("name", g.getControllerName());
            gNode.put("desc", g.getControllerDesc());
            gNode.put("status", g.getControllerStatus());
            gNode.put("type", "group");
            List<Map<String, Object>> children = new ArrayList<>();
            for (ApiUrl u : urlsByGroup.getOrDefault(g.getGroupId(), List.of())) {
                children.add(apiNode(u));
            }
            gNode.put("children", children);
            tree.add(gNode);
        }
        return tree;
    }

    public Map<String, Object> apiNode(ApiUrl u) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("id", "u" + u.getUrlId());
        n.put("urlId", u.getUrlId());
        n.put("groupId", u.getGroupId());
        n.put("label", u.getMethodName());
        n.put("url", u.getRequestUrl());
        n.put("method", u.getMethodType());
        n.put("contentType", u.getMethodContentType());
        n.put("status", u.getUrlStatus());
        n.put("label2", u.getUrlLabel());
        n.put("desc", u.getMethodDesc());
        n.put("sourceType", u.getSourceType());
        n.put("type", "api");
        return n;
    }

    /* ---------- CRUD ---------- */

    public ApiUrl getById(Long urlId) {
        ApiUrl url = urlDao.selectById(urlId);
        if (url == null) {
            throw new BizException(404, "接口不存在");
        }
        return url;
    }

    @Transactional
    public ApiUrl create(ApiUrl url) {
        if (url.getGroupId() == null || url.getRequestUrl() == null || url.getMethodType() == null) {
            throw new BizException(400, "分组、请求路径、请求方法不能为空");
        }
        ApiGroup group = groupDao.selectById(url.getGroupId());
        if (group == null) {
            throw new BizException(404, "所属分组不存在");
        }
        url.setProjectId(group.getProjectId());
        if (url.getMethodName() == null || url.getMethodName().isBlank()) {
            url.setMethodName(url.getMethodType() + " " + url.getRequestUrl());
        }
        checkUnique(url.getGroupId(), url.getMethodType(), url.getRequestUrl(), null);
        url.setSourceType(2); // 手工新增
        urlDao.insert(url);
        return url;
    }

    @Transactional
    public void update(ApiUrl url) {
        ApiUrl db = getById(url.getUrlId());
        if (url.getRequestUrl() != null && url.getMethodType() != null) {
            checkUnique(db.getGroupId(), url.getMethodType(), url.getRequestUrl(), url.getUrlId());
        }
        urlDao.update(url);
        // 同步接口改为手工维护后标记来源
        if (Integer.valueOf(1).equals(db.getSourceType())) {
            ApiUrl mark = new ApiUrl();
            mark.setUrlId(url.getUrlId());
            mark.setSourceType(2);
            urlDao.update(mark);
        }
    }

    @Transactional
    public void updateStatus(Long urlId, Integer status) {
        getById(urlId);
        ApiUrl u = new ApiUrl();
        u.setUrlId(urlId);
        u.setUrlStatus(status);
        urlDao.update(u);
    }

    @Transactional
    public void delete(Long urlId) {
        getById(urlId);
        urlDao.logicDeleteById(urlId);
        ApiUrlDoc doc = docDao.selectByUrlId(urlId);
        if (doc != null) {
            doc.setDelFlag(1);
            docDao.upsert(doc);
        }
    }

    public List<ApiUrl> search(Long projectId, String keyword) {
        return urlDao.search(projectId, keyword);
    }

    private void checkUnique(Long groupId, String methodType, String requestUrl, Long excludeUrlId) {
        ApiUrl exists = urlDao.selectByGroupAndUrl(groupId, methodType, requestUrl);
        if (exists != null && !exists.getUrlId().equals(excludeUrlId)) {
            throw new BizException(400, "该分组下已存在相同方法与路径的接口：" + methodType + " " + requestUrl);
        }
    }
}
