package com.guonl.dto;

import lombok.Data;

import java.util.List;

/**
 * 调试请求
 */
@Data
public class DebugRequest {

    private Long projectId;

    private Long envId;

    /** 关联接口（可空=自由调试） */
    private Long urlId;

    private String method;

    /** 路径或完整 URL，支持 {base} 占位环境 BaseURL */
    private String url;

    private List<KV> headers;

    private List<KV> query;

    /** none/json/form-data/x-www-form-urlencoded */
    private String bodyType;

    private String body;

    @Data
    public static class KV {
        private String key;
        private String value;

        public KV() { }

        public KV(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
