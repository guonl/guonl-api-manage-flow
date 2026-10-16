package com.guonl.dao;

import com.guonl.entity.ApiProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiProjectDao {

    int insert(ApiProject project);

    int update(ApiProject project);

    int logicDeleteById(@Param("projectId") Long projectId);

    ApiProject selectById(@Param("projectId") Long projectId);

    List<ApiProject> selectPage(@Param("keyword") String keyword,
                                @Param("status") Integer status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    long countPage(@Param("keyword") String keyword, @Param("status") Integer status);

    List<ApiProject> selectAll();

    /** 工作台统计：项目总数 */
    long countAll();
}
