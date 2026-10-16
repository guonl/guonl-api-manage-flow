package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * actuator 同步历史
 */
@Data
public class ApiSyncLog {

    private Long syncId;

    private Long projectId;

    /** SUCCESS/FAILED */
    private String syncStatus;

    private Integer addCount;

    private Integer updateCount;

    private Integer offlineCount;

    private String failMsg;

    private String operator;

    private LocalDateTime createTime;
}
