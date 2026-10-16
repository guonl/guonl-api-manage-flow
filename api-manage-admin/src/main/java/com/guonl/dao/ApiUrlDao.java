package com.guonl.dao;

import com.guonl.entity.ApiUrl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiUrlDao {

    int insert(ApiUrl apiUrl);

    int update(ApiUrl apiUrl);

    int logicDeleteById(@Param("urlId") Long urlId);

    ApiUrl selectById(@Param("urlId") Long urlId);

    List<ApiUrl> selectByGroup(@Param("groupId") Long groupId);

    /** 树查询：按项目查全部接口（含分组信息） */
    List<ApiUrl> selectByProject(@Param("projectId") Long projectId);

    /** 同步 diff 用：查分组下全部接口（含已下线） */
    List<ApiUrl> selectAllByGroup(@Param("groupId") Long groupId);

    /** 唯一键查询：group + method + url */
    ApiUrl selectByGroupAndUrl(@Param("groupId") Long groupId,
                               @Param("methodType") String methodType,
                               @Param("requestUrl") String requestUrl);

    /** 关键字检索（方法名/路径/描述/标签） */
    List<ApiUrl> search(@Param("projectId") Long projectId, @Param("keyword") String keyword);

    /** 同步后批量刷新冗余项目id与同步时间 */
    int markSyncTime(@Param("urlId") Long urlId);

    /** 按分组统计接口数 */
    long countByGroup(@Param("groupId") Long groupId);

    /** 工作台统计：接口总数 */
    long countAll();
}
