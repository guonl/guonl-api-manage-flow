package com.guonl.dao;

import com.guonl.entity.FlowRun;
import com.guonl.entity.FlowRunStep;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FlowRunDao {

    int insert(FlowRun run);

    /** 运行结束更新终态 */
    int updateFinish(FlowRun run);

    FlowRun selectById(@Param("runId") Long runId);

    List<FlowRun> selectPage(@Param("flowId") Long flowId,
                             @Param("offset") int offset,
                             @Param("pageSize") int pageSize);

    int countPage(@Param("flowId") Long flowId);

    int insertStep(FlowRunStep step);

    int updateStep(FlowRunStep step);

    List<FlowRunStep> selectStepsByRunId(@Param("runId") Long runId);

    FlowRunStep selectStepById(@Param("runStepId") Long runStepId);

    /** 工作台：最近 N 次流程运行 */
    List<FlowRun> selectRecent(@Param("limit") int limit);
}
