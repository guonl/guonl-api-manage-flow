package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiEnvDao;
import com.guonl.dao.ApiProjectDao;
import com.guonl.entity.ApiEnv;
import com.guonl.entity.ApiProject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnvService {

    private final ApiEnvDao envDao;
    private final ApiProjectDao projectDao;

    public List<ApiEnv> listByProject(Long projectId) {
        return envDao.selectByProject(projectId);
    }

    @Transactional
    public ApiEnv create(ApiEnv env) {
        if (env.getProjectId() == null || env.getEnvName() == null || env.getBaseUrl() == null) {
            throw new BizException(400, "项目id、环境名、Base URL 不能为空");
        }
        validateBaseUrl(env.getBaseUrl());
        if (Integer.valueOf(1).equals(env.getIsDefault())) {
            envDao.clearDefault(env.getProjectId(), null);
        }
        envDao.insert(env);
        // 若项目尚无默认环境，自动设为默认
        if (!Integer.valueOf(1).equals(env.getIsDefault())
                && projectDao.selectById(env.getProjectId()) != null
                && envDao.selectByProject(env.getProjectId()).stream().noneMatch(e -> Integer.valueOf(1).equals(e.getIsDefault()))) {
            env.setIsDefault(1);
            envDao.update(env);
        }
        return env;
    }

    @Transactional
    public void update(ApiEnv env) {
        ApiEnv db = envDao.selectById(env.getEnvId());
        if (db == null) {
            throw new BizException(404, "环境不存在");
        }
        if (env.getBaseUrl() != null) {
            validateBaseUrl(env.getBaseUrl());
        }
        if (Integer.valueOf(1).equals(env.getIsDefault())) {
            envDao.clearDefault(db.getProjectId(), env.getEnvId());
        }
        envDao.update(env);
    }

    @Transactional
    public void delete(Long envId) {
        ApiEnv db = envDao.selectById(envId);
        if (db == null) {
            throw new BizException(404, "环境不存在");
        }
        envDao.logicDeleteById(envId);
    }

    private void validateBaseUrl(String baseUrl) {
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new BizException(400, "Base URL 必须以 http:// 或 https:// 开头");
        }
    }
}
