package com.guonl.dao;

import com.guonl.entity.ApiGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiGroupDao {

    int insert(ApiGroup group);

    int update(ApiGroup group);

    int logicDeleteById(@Param("groupId") Long groupId);

    ApiGroup selectById(@Param("groupId") Long groupId);

    List<ApiGroup> selectByProject(@Param("projectId") Long projectId);

    ApiGroup selectByName(@Param("projectId") Long projectId, @Param("controllerName") String controllerName);
}
