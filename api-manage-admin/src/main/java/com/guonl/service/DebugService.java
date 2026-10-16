package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiEnvDao;
import com.guonl.dao.ApiTestRecordDao;
import com.guonl.dao.ApiUrlDao;
import com.guonl.dto.DebugRequest;
import com.guonl.entity.ApiEnv;
import com.guonl.entity.ApiTestRecord;
import com.guonl.entity.ApiUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口调试：代理发送 HTTP 请求并记录调用历史
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebugService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final ApiEnvDao envDao;
    private final ApiUrlDao urlDao;
    private final ApiTestRecordDao recordDao;

    /** 历史记录响应体截断大小（字节） */
    @Value("${debug.max-body-log-size:65536}")
    private int maxBodyLogSize;

    public Map<String, Object> send(DebugRequest req, String operator) {
        if (req.getEnvId() == null) {
            throw new BizException(400, "请选择调试环境");
        }
        ApiEnv env = envDao.selectById(req.getEnvId());
        if (env == null) {
            throw new BizException(404, "环境不存在");
        }

        // 关联接口时以库内定义为兜底
        ApiUrl apiUrl = null;
        if (req.getUrlId() != null) {
            apiUrl = urlDao.selectById(req.getUrlId());
            if (apiUrl == null) {
                throw new BizException(404, "接口不存在");
            }
        }
        String method = StringUtils.hasText(req.getMethod()) ? req.getMethod().toUpperCase()
                : (apiUrl != null && StringUtils.hasText(apiUrl.getMethodType()) ? apiUrl.getMethodType().toUpperCase() : "GET");
        String url = StringUtils.hasText(req.getUrl()) ? req.getUrl().trim()
                : (apiUrl != null ? apiUrl.getRequestUrl() : null);
        if (!StringUtils.hasText(url)) {
            throw new BizException(400, "请求 URL 不能为空");
        }
        String bodyType = StringUtils.hasText(req.getBodyType()) ? req.getBodyType()
                : (apiUrl != null && StringUtils.hasText(apiUrl.getMethodContentType()) ? apiUrl.getMethodContentType() : "none");

        String finalUrl = buildFinalUrl(env.getBaseUrl(), url, req.getQuery());

        HttpHeaders headers = new HttpHeaders();
        if (req.getHeaders() != null) {
            for (DebugRequest.KV kv : req.getHeaders()) {
                if (StringUtils.hasText(kv.getKey())) {
                    headers.add(kv.getKey(), kv.getValue() == null ? "" : kv.getValue());
                }
            }
        }
        boolean hasBody = !"GET".equals(method) && !"HEAD".equals(method) && !"none".equals(bodyType) && StringUtils.hasText(req.getBody());
        if (hasBody) {
            if (headers.getContentType() == null) {
                headers.setContentType(resolveContentType(bodyType));
            }
        }
        HttpEntity<String> entity = new HttpEntity<>(hasBody ? req.getBody() : null, headers);

        long start = System.currentTimeMillis();
        int status = 0;
        String respBody = null;
        Map<String, String> respHeaders = new LinkedHashMap<>();
        String errorMsg = null;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(finalUrl, HttpMethod.valueOf(method), entity, String.class);
            status = resp.getStatusCode().value();
            respBody = resp.getBody();
            respHeaders = toSingleMap(resp.getHeaders());
        } catch (RestClientResponseException e) {
            // 4xx/5xx 也算有效响应，正常展示给用户
            status = e.getStatusCode().value();
            respBody = e.getResponseBodyAsString();
            respHeaders = toSingleMap(e.getResponseHeaders());
        } catch (ResourceAccessException e) {
            errorMsg = "连接失败：" + rootMessage(e);
        } catch (Exception e) {
            errorMsg = "请求异常：" + rootMessage(e);
        }
        long costMs = System.currentTimeMillis() - start;

        boolean success = status >= 200 && status < 300 && errorMsg == null;
        int size = respBody == null ? 0 : respBody.getBytes(StandardCharsets.UTF_8).length;

        // 落调用历史
        ApiTestRecord record = new ApiTestRecord();
        record.setProjectId(req.getProjectId() != null ? req.getProjectId() : (apiUrl != null ? apiUrl.getProjectId() : null));
        record.setEnvId(req.getEnvId());
        record.setUrlId(req.getUrlId());
        record.setRequestMethod(method);
        record.setRequestUrl(finalUrl);
        record.setRequestHeaders(toJson(req.getHeaders()));
        record.setRequestQuery(toJson(req.getQuery()));
        record.setRequestBody(req.getBody());
        record.setBodyType(bodyType);
        record.setResponseStatus(status);
        record.setResponseHeaders(toJson(respHeaders));
        record.setResponseBody(truncate(respBody));
        record.setCostMs((int) costMs);
        record.setSuccess(success ? 1 : 0);
        record.setErrorMsg(errorMsg == null ? null : errorMsg.substring(0, Math.min(errorMsg.length(), 500)));
        record.setOperator(operator);
        recordDao.insert(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recordId", record.getRecordId());
        result.put("status", status);
        result.put("costMs", costMs);
        result.put("size", size);
        result.put("headers", respHeaders);
        result.put("body", respBody);
        result.put("success", success);
        result.put("errorMsg", errorMsg);
        return result;
    }

    /** 调试模板：接口定义 + 最近一次请求 */
    public Map<String, Object> template(Long urlId) {
        ApiUrl apiUrl = urlDao.selectById(urlId);
        if (apiUrl == null) {
            throw new BizException(404, "接口不存在");
        }
        ApiTestRecord last = recordDao.selectLatestByUrlId(urlId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("api", apiUrl);
        result.put("lastRecord", last);
        return result;
    }

    public List<ApiTestRecord> records(Long urlId, Long projectId, int offset, int pageSize) {
        return recordDao.selectPage(urlId, projectId, offset, Math.min(pageSize, 50));
    }

    public int countRecords(Long urlId, Long projectId) {
        return recordDao.countPage(urlId, projectId);
    }

    public List<ApiTestRecord> recent(int limit) {
        return recordDao.selectRecent(limit);
    }

    public ApiTestRecord record(Long recordId) {
        ApiTestRecord r = recordDao.selectById(recordId);
        if (r == null) {
            throw new BizException(404, "记录不存在");
        }
        return r;
    }

    private String buildFinalUrl(String baseUrl, String url, List<DebugRequest.KV> query) {
        String finalUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            finalUrl = url;
        } else if (url.contains("{base}")) {
            finalUrl = url.replace("{base}", trimSlash(baseUrl));
        } else {
            finalUrl = trimSlash(baseUrl) + (url.startsWith("/") ? url : "/" + url);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(finalUrl);
        if (query != null) {
            for (DebugRequest.KV kv : query) {
                if (StringUtils.hasText(kv.getKey())) {
                    builder.queryParam(kv.getKey(), kv.getValue() == null ? "" : kv.getValue());
                }
            }
        }
        return builder.build().encode().toUriString();
    }

    private String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.replaceAll("/+$", "");
    }

    private MediaType resolveContentType(String bodyType) {
        return switch (bodyType) {
            case "json" -> MediaType.APPLICATION_JSON;
            case "form-data" -> MediaType.MULTIPART_FORM_DATA;
            case "x-www-form-urlencoded" -> MediaType.APPLICATION_FORM_URLENCODED;
            default -> MediaType.TEXT_PLAIN;
        };
    }

    private Map<String, String> toSingleMap(HttpHeaders httpHeaders) {
        Map<String, String> map = new LinkedHashMap<>();
        if (httpHeaders != null) {
            httpHeaders.forEach((k, v) -> map.put(k, v.isEmpty() ? "" : v.get(0)));
        }
        return map;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String body) {
        if (body == null || body.length() <= maxBodyLogSize) {
            return body;
        }
        return body.substring(0, maxBodyLogSize) + "\n...[已截断，原始大小 " + body.length() + " 字符]";
    }

    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() == null ? e.getClass().getSimpleName() : t.getMessage();
    }
}
