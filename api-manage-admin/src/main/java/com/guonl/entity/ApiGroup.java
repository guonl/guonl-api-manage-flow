package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口分组
 */
@Data
public class ApiGroup {

    private Long groupId;

    private Long projectId;

    /** controller全名 */
    private String controllerName;

    /** controller简称（分组显示名） */
    private String controllerShortName;

    private String controllerDesc;

    /** 0-禁用 1-启用 */
    private Integer controllerStatus;

    private Integer groupSort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
