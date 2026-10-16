package com.guonl.dto;

import lombok.Data;

import java.util.Map;

/**
 * /actuator/api-manage 返回的单条接口信息（与 tools 模块 ApiUrlMappings 对应）
 */
@Data
public class RemoteApiInfo {

    private String controllerName;

    private String requestUrl;

    private String methodName;

    private String methodType;

    private String methodContentType;

    private Map<String, Object> requestParamMap;

    private String returnParamClass;

    private Map<String, Object> returnParamMap;
}
