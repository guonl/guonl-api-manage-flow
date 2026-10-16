package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目
 */
@Data
public class ApiProject {

    private Long projectId;

    private String projectName;

    private String projectDesc;

    /** actuator管理地址，如 http://host:port/actuator */
    private String manageUrl;

    /** 0-禁用 1-启用 */
    private Integer projectStatus;

    private String owner;

    private String systemName;

    private Long defaultEnvId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
