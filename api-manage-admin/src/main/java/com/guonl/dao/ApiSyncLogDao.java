package com.guonl.dao;

import com.guonl.entity.ApiSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiSyncLogDao {

    int insert(ApiSyncLog log);

    List<ApiSyncLog> selectByProject(@Param("projectId") Long projectId, @Param("limit") int limit);
}
