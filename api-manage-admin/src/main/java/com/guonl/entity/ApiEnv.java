package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目环境
 */
@Data
public class ApiEnv {

    private Long envId;

    private Long projectId;

    private String envName;

    /** Base URL http(s)://host[:port][/prefix] */
    private String baseUrl;

    private String envDesc;

    /** 1-默认环境 */
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
