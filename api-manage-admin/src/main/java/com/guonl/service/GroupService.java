package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiGroupDao;
import com.guonl.dao.ApiUrlDao;
import com.guonl.entity.ApiGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final ApiGroupDao groupDao;
    private final ApiUrlDao urlDao;

    public List<ApiGroup> listByProject(Long projectId) {
        return groupDao.selectByProject(projectId);
    }

    public ApiGroup getById(Long groupId) {
        ApiGroup group = groupDao.selectById(groupId);
        if (group == null) {
            throw new BizException(404, "分组不存在");
        }
        return group;
    }

    public ApiGroup create(ApiGroup group) {
        if (group.getProjectId() == null || group.getControllerName() == null || group.getControllerShortName() == null) {
            throw new BizException(400, "项目id、controller全名、分组名不能为空");
        }
        ApiGroup exists = groupDao.selectByName(group.getProjectId(), group.getControllerName());
        if (exists != null) {
            throw new BizException(400, "该项目下已存在同名 controller 分组：" + group.getControllerName());
        }
        groupDao.insert(group);
        return group;
    }

    public void update(ApiGroup group) {
        getById(group.getGroupId());
        groupDao.update(group);
    }

    @Transactional
    public void delete(Long groupId) {
        getById(groupId);
        long apiCount = urlDao.countByGroup(groupId);
        if (apiCount > 0) {
            throw new BizException(400, "分组下仍有 " + apiCount + " 个接口，请先清空或迁移");
        }
        groupDao.logicDeleteById(groupId);
    }
}
