package com.guonl.dao;

import com.guonl.entity.FlowStep;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowStepDao {

    int insert(FlowStep step);

    int deleteByFlowId(@Param("flowId") Long flowId);

    List<FlowStep> listByFlowId(@Param("flowId") Long flowId);
}
