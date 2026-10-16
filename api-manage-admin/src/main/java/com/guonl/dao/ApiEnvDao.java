package com.guonl.dao;

import com.guonl.entity.ApiEnv;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiEnvDao {

    int insert(ApiEnv env);

    int update(ApiEnv env);

    int logicDeleteById(@Param("envId") Long envId);

    ApiEnv selectById(@Param("envId") Long envId);

    List<ApiEnv> selectByProject(@Param("projectId") Long projectId);

    /** 清除项目下其他环境的默认标记 */
    int clearDefault(@Param("projectId") Long projectId, @Param("excludeEnvId") Long excludeEnvId);
}
