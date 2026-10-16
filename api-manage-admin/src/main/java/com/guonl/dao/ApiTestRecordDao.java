package com.guonl.dao;

import com.guonl.entity.ApiTestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 调试历史 DAO
 */
@Mapper
public interface ApiTestRecordDao {

    int insert(ApiTestRecord record);

    List<ApiTestRecord> selectPage(@Param("urlId") Long urlId,
                                   @Param("projectId") Long projectId,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    int countPage(@Param("urlId") Long urlId, @Param("projectId") Long projectId);

    /** 最近调用（工作台） */
    List<ApiTestRecord> selectRecent(@Param("limit") int limit);

    ApiTestRecord selectById(@Param("recordId") Long recordId);

    /** 某接口最近一次调试请求（调试模板回填） */
    ApiTestRecord selectLatestByUrlId(@Param("urlId") Long urlId);

    /** 工作台统计：今日调试总数 */
    long countToday();

    /** 工作台统计：今日调试失败数 */
    long countTodayFail();
}
