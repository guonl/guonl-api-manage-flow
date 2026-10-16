package com.guonl.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置KV
 */
@Data
public class SysConfig {

    private String configKey;

    private String configValue;

    private String configDesc;

    private LocalDateTime updateTime;
}
