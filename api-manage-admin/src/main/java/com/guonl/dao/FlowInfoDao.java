package com.guonl.dao;

import com.guonl.entity.FlowInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowInfoDao {

    int insert(FlowInfo flow);

    int update(FlowInfo flow);

    int deleteById(@Param("flowId") Long flowId);

    FlowInfo selectById(@Param("flowId") Long flowId);

    List<FlowInfo> selectPage(@Param("projectId") Long projectId,
                              @Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("pageSize") int pageSize);

    int countPage(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /** 工作台统计：流程总数 */
    long countAll();
}
