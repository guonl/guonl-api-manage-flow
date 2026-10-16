package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.common.BizException;
import com.guonl.common.Constants;
import com.guonl.dao.ApiProjectDao;
import com.guonl.dao.ApiTestRecordDao;
import com.guonl.dao.ApiUrlDao;
import com.guonl.dao.FlowInfoDao;
import com.guonl.dao.FlowRunDao;
import com.guonl.dao.SysConfigDao;
import com.guonl.entity.SysConfig;
import com.guonl.service.AiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统设置：KV 配置读写（api-key 脱敏）、登录密码修改
 */
@RestController
@RequestMapping("/api/sys")
@RequiredArgsConstructor
public class SysController {

    private final SysConfigDao sysConfigDao;
    private final AiService aiService;
    private final ApiProjectDao apiProjectDao;
    private final ApiUrlDao apiUrlDao;
    private final FlowInfoDao flowInfoDao;
    private final ApiTestRecordDao apiTestRecordDao;
    private final FlowRunDao flowRunDao;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 敏感 key：查询时脱敏，且不允许通过配置保存接口直接修改 */
    private static final Set<String> SENSITIVE_KEYS = Set.of(Constants.CFG_AI_API_KEY, Constants.CFG_PASSWORD);

    /**
     * 查询配置。keys 为空返回全部；敏感项（api-key/密码）值脱敏。
     */
    @GetMapping("/config")
    public ApiResult<List<SysConfig>> config(@RequestParam(required = false) String keys) {
        List<SysConfig> list;
        if (StringUtils.hasText(keys)) {
            list = Arrays.stream(keys.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(sysConfigDao::selectByKey)
                    .toList();
        } else {
            list = sysConfigDao.selectAll();
        }
        list.forEach(c -> {
            if (SENSITIVE_KEYS.contains(c.getConfigKey()) && StringUtils.hasText(c.getConfigValue())) {
                c.setConfigValue(Constants.CFG_PASSWORD.equals(c.getConfigKey()) ? "******" : mask(c.getConfigValue()));
            }
        });
        return ApiResult.ok(list);
    }

    /**
     * 批量保存配置。api-key 若为脱敏值/空串则跳过不覆盖；密码只能走 /api/sys/password。
     */
    @PostMapping("/config")
    public ApiResult<Void> saveConfig(@RequestBody List<ConfigItem> items) {
        for (ConfigItem item : items) {
            if (!StringUtils.hasText(item.getConfigKey())) {
                continue;
            }
            String value = item.getConfigValue();
            if (SENSITIVE_KEYS.contains(item.getConfigKey())
                    && (Constants.CFG_PASSWORD.equals(item.getConfigKey())
                        || value == null || value.isBlank() || mask(value).equals(value))) {
                continue;
            }
            sysConfigDao.upsert(item.getConfigKey(), value == null ? "" : value.trim(), item.getConfigDesc());
        }
        aiService.evictClient();
        return ApiResult.ok();
    }

    /**
     * 修改登录密码
     */
    @PostMapping("/password")
    public ApiResult<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        SysConfig current = sysConfigDao.selectByKey(Constants.CFG_PASSWORD);
        String hashed = current != null ? current.getConfigValue() : null;
        if (!StringUtils.hasText(hashed) || !passwordEncoder.matches(req.getOldPassword(), hashed)) {
            throw new BizException("原密码不正确");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6) {
            throw new BizException("新密码长度至少 6 位");
        }
        sysConfigDao.upsert(Constants.CFG_PASSWORD, passwordEncoder.encode(req.getNewPassword()), "登录密码(BCrypt)");
        return ApiResult.ok();
    }

    /**
     * 工作台统计：资源计数 + 今日调试概况 + 最近调试/运行记录
     */
    @GetMapping("/dashboard")
    public ApiResult<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projectCount", apiProjectDao.countAll());
        data.put("apiCount", apiUrlDao.countAll());
        data.put("flowCount", flowInfoDao.countAll());
        data.put("todayDebugCount", apiTestRecordDao.countToday());
        data.put("todayFailCount", apiTestRecordDao.countTodayFail());
        data.put("recentDebugs", apiTestRecordDao.selectRecent(8));
        data.put("recentRuns", flowRunDao.selectRecent(8));
        return ApiResult.ok(data);
    }

    private String mask(String value) {
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 6) + "****" + value.substring(value.length() - 4);
    }

    @Data
    public static class ConfigItem {
        private String configKey;
        private String configValue;
        private String configDesc;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
