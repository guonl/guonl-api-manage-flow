package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiGroupDao;
import com.guonl.dao.ApiProjectDao;
import com.guonl.dao.ApiSyncLogDao;
import com.guonl.dao.ApiUrlDao;
import com.guonl.dto.RemoteApiInfo;
import com.guonl.entity.ApiGroup;
import com.guonl.entity.ApiProject;
import com.guonl.entity.ApiSyncLog;
import com.guonl.entity.ApiUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * actuator 接口同步：拉取宿主应用 /actuator/api-manage 并与库内接口做 diff
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final ApiProjectDao projectDao;
    private final ApiGroupDao groupDao;
    private final ApiUrlDao urlDao;
    private final ApiSyncLogDao syncLogDao;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sync(Long projectId, String operator) {
        ApiProject project = projectDao.selectById(projectId);
        if (project == null) {
            throw new BizException(404, "项目不存在");
        }
        String manageUrl = project.getManageUrl();
        if (manageUrl == null || manageUrl.isBlank()) {
            throw new BizException(400, "项目未配置 actuator 管理地址（如 http://host:port/actuator）");
        }
        String endpoint = manageUrl.replaceAll("/+$", "") + "/api-manage";

        List<RemoteApiInfo> remoteList;
        try {
            String body = restTemplate.getForObject(endpoint, String.class);
            remoteList = JSON_MAPPER.readValue(body, new TypeReference<List<RemoteApiInfo>>() { });
        } catch (Exception e) {
            saveLog(projectId, "FAILED", 0, 0, 0, truncate(e.getMessage()), operator);
            throw new BizException(502, "拉取接口信息失败：" + truncate(e.getMessage()));
        }
        if (remoteList == null) {
            remoteList = List.of();
        }

        // 按 controller 分组
        Map<String, List<RemoteApiInfo>> byController = new LinkedHashMap<>();
        for (RemoteApiInfo info : remoteList) {
            if (info.getControllerName() == null || info.getRequestUrl() == null || info.getMethodType() == null) {
                continue;
            }
            byController.computeIfAbsent(info.getControllerName(), k -> new ArrayList<>()).add(info);
        }

        int addCount = 0;
        int updateCount = 0;
        int offlineCount = 0;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (Map.Entry<String, List<RemoteApiInfo>> entry : byController.entrySet()) {
            String controllerName = entry.getKey();
            ApiGroup group = groupDao.selectByName(projectId, controllerName);
            if (group == null) {
                group = new ApiGroup();
                group.setProjectId(projectId);
                group.setControllerName(controllerName);
                group.setControllerShortName(controllerName.substring(controllerName.lastIndexOf('.') + 1));
                group.setControllerDesc("actuator 同步创建");
                group.setGroupSort(100);
                groupDao.insert(group);
            }

            List<ApiUrl> existing = urlDao.selectAllByGroup(group.getGroupId());
            Map<String, ApiUrl> existingByKey = new HashMap<>();
            for (ApiUrl u : existing) {
                existingByKey.put(u.getMethodType().toUpperCase() + " " + u.getRequestUrl(), u);
            }

            Set<Long> touched = new HashSet<>();
            for (RemoteApiInfo info : entry.getValue()) {
                String methodType = info.getMethodType().toUpperCase();
                ApiUrl db = existingByKey.get(methodType + " " + info.getRequestUrl());
                if (db != null) {
                    ApiUrl up = new ApiUrl();
                    up.setUrlId(db.getUrlId());
                    up.setMethodName(info.getMethodName());
                    up.setMethodContentType(info.getMethodContentType() == null ? "none" : info.getMethodContentType());
                    up.setRequestParamMap(toJson(info.getRequestParamMap()));
                    up.setReturnParamClass(info.getReturnParamClass());
                    up.setReturnParamMap(toJson(info.getReturnParamMap()));
                    up.setLastSyncTime(now);
                    // 已下线的接口重新出现则恢复启用
                    if (Integer.valueOf(2).equals(db.getUrlStatus())) {
                        up.setUrlStatus(1);
                    }
                    urlDao.update(up);
                    urlDao.markSyncTime(db.getUrlId());
                    touched.add(db.getUrlId());
                    updateCount++;
                } else {
                    ApiUrl ins = new ApiUrl();
                    ins.setGroupId(group.getGroupId());
                    ins.setProjectId(projectId);
                    ins.setRequestUrl(info.getRequestUrl());
                    ins.setMethodName(info.getMethodName());
                    ins.setMethodType(methodType);
                    ins.setMethodContentType(info.getMethodContentType() == null ? "none" : info.getMethodContentType());
                    ins.setRequestParamMap(toJson(info.getRequestParamMap()));
                    ins.setReturnParamClass(info.getReturnParamClass());
                    ins.setReturnParamMap(toJson(info.getReturnParamMap()));
                    ins.setUrlStatus(1);
                    ins.setSourceType(1);
                    ins.setLastSyncTime(now);
                    urlDao.insert(ins);
                    addCount++;
                }
            }

            // 下线 diff：同步来源、未被本次拉取覆盖、且未标记下线的接口 → 已下线
            for (ApiUrl u : existing) {
                if (Integer.valueOf(1).equals(u.getSourceType())
                        && !touched.contains(u.getUrlId())
                        && !Integer.valueOf(2).equals(u.getUrlStatus())) {
                    ApiUrl off = new ApiUrl();
                    off.setUrlId(u.getUrlId());
                    off.setUrlStatus(2);
                    urlDao.update(off);
                    offlineCount++;
                }
            }
        }

        saveLog(projectId, "SUCCESS", addCount, updateCount, offlineCount, null, operator);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("addCount", addCount);
        result.put("updateCount", updateCount);
        result.put("offlineCount", offlineCount);
        result.put("remoteCount", remoteList.size());
        return result;
    }

    public List<ApiSyncLog> logs(Long projectId, int limit) {
        return syncLogDao.selectByProject(projectId, Math.min(limit, 100));
    }

    private void saveLog(Long projectId, String status, int add, int update, int offline, String failMsg, String operator) {
        ApiSyncLog syncLog = new ApiSyncLog();
        syncLog.setProjectId(projectId);
        syncLog.setSyncStatus(status);
        syncLog.setAddCount(add);
        syncLog.setUpdateCount(update);
        syncLog.setOfflineCount(offline);
        syncLog.setFailMsg(failMsg);
        syncLog.setOperator(operator);
        syncLogDao.insert(syncLog);
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String msg) {
        if (msg == null) {
            return "未知异常";
        }
        return msg.length() > 480 ? msg.substring(0, 480) : msg;
    }
}
