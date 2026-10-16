package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.common.PageResult;
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
public class ProjectService {

    private final ApiProjectDao projectDao;
    private final ApiEnvDao envDao;

    public PageResult<ApiProject> page(String keyword, Integer status, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        List<ApiProject> list = projectDao.selectPage(keyword, status, offset, pageSize);
        long total = projectDao.countPage(keyword, status);
        return new PageResult<>(total, list);
    }

    public List<ApiProject> listAll() {
        return projectDao.selectAll();
    }

    public ApiProject getById(Long projectId) {
        ApiProject project = projectDao.selectById(projectId);
        if (project == null) {
            throw new BizException(404, "项目不存在");
        }
        return project;
    }

    @Transactional
    public ApiProject create(ApiProject project) {
        if (project.getProjectName() == null || project.getProjectName().isBlank()) {
            throw new BizException(400, "项目名称不能为空");
        }
        projectDao.insert(project);
        // 默认创建一个 dev 环境，作为调试起步
        ApiEnv dev = new ApiEnv();
        dev.setProjectId(project.getProjectId());
        dev.setEnvName("dev");
        dev.setBaseUrl("http://localhost:8080");
        dev.setIsDefault(1);
        dev.setEnvDesc("创建项目时自动生成的默认环境");
        envDao.insert(dev);
        project.setDefaultEnvId(dev.getEnvId());
        projectDao.update(project);
        return project;
    }

    @Transactional
    public void update(ApiProject project) {
        getById(project.getProjectId());
        projectDao.update(project);
    }

    @Transactional
    public void delete(Long projectId) {
        getById(projectId);
        projectDao.logicDeleteById(projectId);
        List<ApiEnv> envs = envDao.selectByProject(projectId);
        envs.forEach(e -> envDao.logicDeleteById(e.getEnvId()));
    }
}
