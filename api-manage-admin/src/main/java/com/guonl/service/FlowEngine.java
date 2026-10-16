package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiEnvDao;
import com.guonl.dao.FlowInfoDao;
import com.guonl.dao.FlowRunDao;
import com.guonl.dao.FlowStepDao;
import com.guonl.entity.ApiEnv;
import com.guonl.entity.FlowInfo;
import com.guonl.entity.FlowRun;
import com.guonl.entity.FlowRunStep;
import com.guonl.entity.FlowStep;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 流程编排引擎：顺序执行步骤，支持 {{var}} 变量渲染、JSONPath 提取、断言与失败策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowEngine {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    /** {{var}} 占位符 */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.\\-]+)\\s*}}");

    private final RestTemplate restTemplate;
    private final FlowInfoDao flowInfoDao;
    private final FlowStepDao flowStepDao;
    private final FlowRunDao flowRunDao;
    private final ApiEnvDao envDao;

    @Value("${debug.max-body-log-size:65536}")
    private int maxBodyLogSize;

    /**
     * 运行流程（同步执行）
     *
     * @param mode DEBUG=失败即停 NORMAL=按步骤 on_failure 策略
     */
    public FlowRun run(Long flowId, Long envId, Map<String, Object> initVars, String mode, String operator) {
        FlowInfo flow = flowInfoDao.selectById(flowId);
        if (flow == null) {
            throw new BizException(404, "流程不存在");
        }
        if (!"1".equals(flow.getFlowStatus())) {
            throw new BizException(400, "流程已停用，无法运行");
        }
        List<FlowStep> steps = flowStepDao.listByFlowId(flowId);
        if (steps.isEmpty()) {
            throw new BizException(400, "流程没有步骤，请先编辑步骤");
        }
        ApiEnv env = resolveEnv(envId, flow.getProjectId());
        if (env == null) {
            throw new BizException(400, "请选择运行环境");
        }

        FlowRun run = new FlowRun();
        run.setFlowId(flowId);
        run.setFlowNameSnapshot(flow.getFlowName());
        run.setTriggerType("DEBUG".equalsIgnoreCase(mode) ? "DEBUG" : "MANUAL");
        run.setInitVars(toJson(initVars));
        run.setRunStatus("RUNNING");
        run.setOperator(operator);
        flowRunDao.insert(run);

        // 上下文：初始变量 + 环境内置变量
        Map<String, Object> context = new LinkedHashMap<>();
        if (initVars != null) {
            context.putAll(initVars);
        }
        context.put("base", trimSlash(env.getBaseUrl()));
        context.put("baseUrl", trimSlash(env.getBaseUrl()));

        long start = System.currentTimeMillis();
        String runError = null;
        boolean debugMode = "DEBUG".equalsIgnoreCase(mode);
        for (FlowStep step : steps) {
            if (!"1".equals(step.getEnabled())) {
                continue;
            }
            FlowRunStep rs = executeStep(run.getRunId(), step, env, context);
            if ("FAILED".equals(rs.getStepStatus())) {
                boolean stop = debugMode || "TERMINATE".equals(step.getOnFailure());
                if (stop) {
                    runError = "步骤[" + step.getStepName() + "]失败" +
                            (rs.getErrorMsg() == null ? "" : "：" + rs.getErrorMsg());
                    break;
                }
            }
        }

        run.setRunStatus(runError == null ? "SUCCESS" : "FAILED");
        run.setTotalCostMs((int) (System.currentTimeMillis() - start));
        run.setErrorMsg(runError == null ? null : abbreviate(runError, 500));
        run.setContextSnapshot(toJson(context));
        flowRunDao.updateFinish(run);
        return run;
    }

    /**
     * 执行单个步骤：渲染请求 → 发送 → 提取 → 断言 → 落明细
     */
    public FlowRunStep executeStep(Long runId, FlowStep step, ApiEnv env, Map<String, Object> context) {
        FlowRunStep rs = new FlowRunStep();
        rs.setRunId(runId);
        rs.setStepId(step.getStepId());
        rs.setStepName(step.getStepName());
        rs.setSortOrder(step.getSortOrder());

        long start = System.currentTimeMillis();
        try {
            if (step.getDelayMs() != null && step.getDelayMs() > 0) {
                Thread.sleep(Math.min(step.getDelayMs(), 60_000));
            }
            if ("DELAY".equals(step.getStepType())) {
                rs.setStepStatus("SUCCESS");
                rs.setCostMs((int) (System.currentTimeMillis() - start));
                rs.setRequestSnapshot(toJson(Map.of("type", "DELAY", "delayMs", step.getDelayMs())));
                flowRunDao.insertStep(rs);
                return rs;
            }

            String rawUrl = StringUtils.hasText(step.getRequestUrl()) ? step.getRequestUrl().trim() : "";
            if (!StringUtils.hasText(rawUrl)) {
                throw new BizException(400, "步骤[" + step.getStepName() + "]请求 URL 不能为空");
            }
            String method = StringUtils.hasText(step.getRequestMethod())
                    ? step.getRequestMethod().toUpperCase() : "GET";
            String bodyType = StringUtils.hasText(step.getBodyType()) ? step.getBodyType() : "none";
            String renderedBody = render(step.getRequestBody(), context);
            Map<String, String> headerMap = renderMap(step.getRequestHeaders(), context);
            Map<String, String> queryMap = renderMap(step.getRequestQuery(), context);

            String finalUrl = buildFinalUrl(render(rawUrl, context), env.getBaseUrl(), queryMap);
            String methodUpper = method;
            boolean hasBody = !"GET".equals(methodUpper) && !"HEAD".equals(methodUpper)
                    && !"none".equals(bodyType) && StringUtils.hasText(renderedBody);

            Map<String, Object> requestSnapshot = new LinkedHashMap<>();
            requestSnapshot.put("method", method);
            requestSnapshot.put("url", finalUrl);
            requestSnapshot.put("headers", headerMap);
            requestSnapshot.put("query", queryMap);
            requestSnapshot.put("body", hasBody ? renderedBody : null);
            rs.setRequestSnapshot(toJson(requestSnapshot));

            HttpHeaders headers = new HttpHeaders();
            headerMap.forEach(headers::add);
            if (hasBody && headers.getContentType() == null) {
                headers.setContentType(resolveContentType(bodyType));
            }
            HttpEntity<String> entity = new HttpEntity<>(hasBody ? renderedBody : null, headers);

            int status = 0;
            String respBody = null;
            String errorMsg = null;
            try {
                ResponseEntity<String> resp = restTemplate.exchange(finalUrl, HttpMethod.valueOf(method), entity, String.class);
                status = resp.getStatusCode().value();
                respBody = resp.getBody();
            } catch (RestClientResponseException e) {
                // 4xx/5xx 也算有效响应，交给断言判定
                status = e.getStatusCode().value();
                respBody = e.getResponseBodyAsString();
            } catch (ResourceAccessException e) {
                errorMsg = "连接失败：" + rootMessage(e);
            }
            rs.setResponseStatus(status);
            rs.setResponseBody(truncate(respBody));

            // 变量提取（无论响应成败都尝试提取，便于断言与后续步骤使用）
            JsonNode respRoot = parseTree(respBody);
            Map<String, Object> extracted = extract(step.getExtractRules(), respRoot);
            if (!extracted.isEmpty()) {
                context.putAll(extracted);
            }
            rs.setExtractResult(extracted.isEmpty() ? null : toJson(extracted));

            // 断言
            List<Map<String, Object>> assertResults = assertStep(step.getAssertions(), respRoot, status, errorMsg);
            rs.setAssertResult(assertResults == null ? null : toJson(assertResults));
            boolean assertsPass = assertResults == null || assertResults.stream().allMatch(a -> Boolean.TRUE.equals(a.get("pass")));
            boolean ok = errorMsg == null && assertsPass && (status >= 200 && status < 300 || hasExplicitStatusAssert(step.getAssertions()));
            if (errorMsg != null) {
                rs.setErrorMsg(abbreviate(errorMsg, 500));
            } else if (!assertsPass) {
                rs.setErrorMsg("断言未通过");
            } else if (!(status >= 200 && status < 300)) {
                rs.setErrorMsg("HTTP " + status);
            }
            rs.setStepStatus(ok ? "SUCCESS" : "FAILED");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rs.setStepStatus("FAILED");
            rs.setErrorMsg("步骤执行被中断");
        } catch (Exception e) {
            rs.setStepStatus("FAILED");
            rs.setErrorMsg(abbreviate("步骤异常：" + rootMessage(e), 500));
        }
        rs.setCostMs((int) (System.currentTimeMillis() - start));
        flowRunDao.insertStep(rs);
        return rs;
    }

    /**
     * 单步重跑：按运行明细快照重发请求（不重算变量与断言）
     */
    public FlowRunStep rerunStep(Long runStepId) {
        FlowRunStep rs = flowRunDao.selectStepById(runStepId);
        if (rs == null) {
            throw new BizException(404, "运行明细不存在");
        }
        FlowRun run = flowRunDao.selectById(rs.getRunId());
        ApiEnv env = null;
        if (run != null) {
            FlowInfo flow = flowInfoDao.selectById(run.getFlowId());
            if (flow != null) {
                env = resolveEnv(null, flow.getProjectId());
            }
        }
        if (env == null) {
            throw new BizException(400, "未找到可用环境");
        }
        Map<String, Object> snapshot = parseMap(rs.getRequestSnapshot());
        String method = String.valueOf(snapshot.getOrDefault("method", "GET"));
        String url = String.valueOf(snapshot.get("url"));
        Map<String, String> headerMap = toStringMap(snapshot.get("headers"));
        String body = snapshot.get("body") == null ? null : String.valueOf(snapshot.get("body"));

        long start = System.currentTimeMillis();
        int status = 0;
        String respBody = null;
        String errorMsg = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headerMap.forEach(headers::add);
            boolean hasBody = !"GET".equals(method) && !"HEAD".equals(method) && body != null;
            if (hasBody && headers.getContentType() == null) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
            HttpEntity<String> entity = new HttpEntity<>(hasBody ? body : null, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.valueOf(method), entity, String.class);
            status = resp.getStatusCode().value();
            respBody = resp.getBody();
        } catch (RestClientResponseException e) {
            status = e.getStatusCode().value();
            respBody = e.getResponseBodyAsString();
        } catch (ResourceAccessException e) {
            errorMsg = "连接失败：" + rootMessage(e);
        } catch (Exception e) {
            errorMsg = "请求异常：" + rootMessage(e);
        }
        rs.setResponseStatus(status);
        rs.setResponseBody(truncate(respBody));
        rs.setErrorMsg(errorMsg == null ? null : abbreviate(errorMsg, 500));
        rs.setStepStatus(errorMsg == null && status >= 200 && status < 300 ? "SUCCESS" : "FAILED");
        rs.setCostMs((int) (System.currentTimeMillis() - start));
        flowRunDao.updateStep(rs);
        return rs;
    }

    // ==================== 变量渲染 ====================

    /** 将模板中的 {{var}} 替换为上下文值；未定义的占位保留原样 */
    public String render(String template, Map<String, Object> context) {
        if (!StringUtils.hasText(template) || context == null || context.isEmpty()) {
            return template;
        }
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = context.get(key);
            String replacement = value == null ? matcher.group(0) : String.valueOf(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 渲染 JSON 模板并解析为 Map（key → value 均做 {{var}} 替换） */
    private Map<String, String> renderMap(String templateJson, Map<String, Object> context) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(templateJson)) {
            return result;
        }
        JsonNode node = parseTree(render(templateJson, context));
        if (node != null && node.isObject()) {
            node.propertyNames().forEach(name -> {
                JsonNode v = node.get(name);
                String value = v == null || v.isNull() ? "" : v.isTextual() ? v.asText() : String.valueOf(v);
                result.put(name, value);
            });
        }
        return result;
    }

    // ==================== JSONPath 提取 ====================

    /**
     * 简化 JSONPath：支持 $.a.b.c、$.list[0].id、$[0]；结果统一转字符串（对象/数组转 JSON）
     */
    public Map<String, Object> extract(String rulesJson, JsonNode respRoot) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(rulesJson) || respRoot == null) {
            return result;
        }
        JsonNode rules = parseTree(rulesJson);
        if (rules == null || !rules.isArray()) {
            return result;
        }
        for (JsonNode rule : rules) {
            String varName = rule.path("varName").asText(null);
            String jsonPath = rule.path("jsonPath").asText(null);
            if (!StringUtils.hasText(varName) || !StringUtils.hasText(jsonPath)) {
                continue;
            }
            JsonNode target = selectPath(respRoot, jsonPath);
            if (target == null || target.isMissingNode()) {
                result.put(varName, null);
            } else if (target.isValueNode()) {
                result.put(varName, target.asText());
            } else {
                result.put(varName, JSON_MAPPER.writeValueAsString(target));
            }
        }
        return result;
    }

    /** 按路径取节点：$.data.list[0].id */
    private JsonNode selectPath(JsonNode root, String path) {
        try {
            String p = path.trim();
            JsonNode current = root;
            if (p.startsWith("$.")) {
                p = p.substring(2);
            } else if (p.equals("$")) {
                return root;
            } else if (!p.startsWith("$[")) {
                p = p.replaceFirst("^\\$?\\.?", "");
            }
            if (p.isEmpty()) {
                return root;
            }
            for (String seg : p.split("\\.")) {
                Matcher m = Pattern.compile("([A-Za-z0-9_\\-]*)(\\[(\\d+)])*").matcher(seg);
                if (!m.matches()) {
                    return null;
                }
                String name = m.group(1);
                if (!name.isEmpty()) {
                    if (current == null || !current.isObject()) {
                        return null;
                    }
                    current = current.get(name);
                }
                // 同段内可带多个 [n]
                Matcher idx = Pattern.compile("\\[(\\d+)]").matcher(seg);
                while (idx.find()) {
                    if (current == null || !current.isArray()) {
                        return null;
                    }
                    current = current.get(Integer.parseInt(idx.group(1)));
                }
                if (current == null) {
                    return null;
                }
            }
            return current;
        } catch (Exception e) {
            log.warn("JSONPath 解析失败: {}", path, e);
            return null;
        }
    }

    // ==================== 断言 ====================

    /**
     * 断言类型：EQUALS / NOT_EQUALS / CONTAINS / NOT_CONTAINS / NOT_NULL /
     * GT / LT / GTE / LTE / STATUS（expect=期望状态码）
     */
    private List<Map<String, Object>> assertStep(String assertionsJson, JsonNode respRoot, int status, String errorMsg) {
        if (!StringUtils.hasText(assertionsJson)) {
            return null;
        }
        JsonNode rules = parseTree(assertionsJson);
        if (rules == null || !rules.isArray() || rules.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode rule : rules) {
            String type = rule.path("type").asText("EQUALS").toUpperCase();
            String target = rule.path("target").asText(null);
            String expect = rule.path("expect").asText(null);
            String desc = rule.path("desc").asText(null);

            String actual;
            if ("STATUS".equals(type)) {
                actual = String.valueOf(status);
            } else if ("NOT_NULL".equals(type)) {
                actual = null;
            } else {
                JsonNode node = selectPath(respRoot, target);
                if (node == null || node.isMissingNode()) {
                    actual = null;
                } else if (node.isValueNode()) {
                    actual = node.asText();
                } else {
                    actual = JSON_MAPPER.writeValueAsString(node);
                }
            }

            boolean pass;
            try {
                pass = switch (type) {
                    case "STATUS" -> expect != null && expect.equals(actual);
                    case "NOT_NULL" -> actual != null && !actual.isEmpty();
                    case "EQUALS" -> actual != null && actual.equals(expect);
                    case "NOT_EQUALS" -> actual == null || !actual.equals(expect);
                    case "CONTAINS" -> actual != null && expect != null && actual.contains(expect);
                    case "NOT_CONTAINS" -> actual == null || expect == null || !actual.contains(expect);
                    case "GT" -> compare(actual, expect) > 0;
                    case "LT" -> compare(actual, expect) < 0;
                    case "GTE" -> compare(actual, expect) >= 0;
                    case "LTE" -> compare(actual, expect) <= 0;
                    default -> false;
                };
            } catch (Exception e) {
                pass = false;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", type);
            item.put("target", target);
            item.put("expect", expect);
            item.put("actual", actual);
            item.put("pass", pass);
            if (StringUtils.hasText(desc)) {
                item.put("desc", desc);
            }
            results.add(item);
        }
        return results;
    }

    /** 断言列表中是否包含 STATUS 类型（用于非 2xx 时仍可判成功） */
    private boolean hasExplicitStatusAssert(String assertionsJson) {
        if (!StringUtils.hasText(assertionsJson)) {
            return false;
        }
        JsonNode rules = parseTree(assertionsJson);
        if (rules == null || !rules.isArray()) {
            return false;
        }
        for (JsonNode rule : rules) {
            if ("STATUS".equalsIgnoreCase(rule.path("type").asText())) {
                return true;
            }
        }
        return false;
    }

    /** 数值优先比较，无法转数值时回退字符串比较 */
    private int compare(String actual, String expect) {
        if (actual == null || expect == null) {
            return -1;
        }
        try {
            return Double.compare(Double.parseDouble(actual.trim()), Double.parseDouble(expect.trim()));
        } catch (NumberFormatException e) {
            return actual.compareTo(expect);
        }
    }

    // ==================== 工具 ====================

    /** 环境解析：优先指定环境，否则项目默认环境，再退首个环境 */
    private ApiEnv resolveEnv(Long envId, Long projectId) {
        if (envId != null) {
            return envDao.selectById(envId);
        }
        List<ApiEnv> envs = projectId == null ? List.of() : envDao.selectByProject(projectId);
        if (envs.isEmpty()) {
            return null;
        }
        return envs.stream().filter(e -> Integer.valueOf(1).equals(e.getIsDefault())).findFirst().orElse(envs.get(0));
    }

    private String buildFinalUrl(String url, String baseUrl, Map<String, String> query) {
        String finalUrl;
        if (url.startsWith("http://") || url.startsWith("https://")) {
            finalUrl = url;
        } else if (url.contains("{base}")) {
            finalUrl = url.replace("{base}", trimSlash(baseUrl));
        } else {
            finalUrl = trimSlash(baseUrl) + (url.startsWith("/") ? url : "/" + url);
        }
        if (query != null && !query.isEmpty()) {
            StringBuilder sb = new StringBuilder(finalUrl);
            boolean first = !finalUrl.contains("?");
            for (Map.Entry<String, String> e : query.entrySet()) {
                sb.append(first ? '?' : '&').append(urlEncode(e.getKey())).append('=').append(urlEncode(e.getValue()));
                first = false;
            }
            finalUrl = sb.toString();
        }
        return finalUrl;
    }

    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
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

    private JsonNode parseTree(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JSON_MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON_MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, String> toStringMap(Object obj) {
        Map<String, String> map = new LinkedHashMap<>();
        if (obj instanceof Map<?, ?> raw) {
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                map.put(String.valueOf(e.getKey()), e.getValue() == null ? "" : String.valueOf(e.getValue()));
            }
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

    private String abbreviate(String s, int max) {
        return s == null ? null : s.substring(0, Math.min(s.length(), max));
    }

    private String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() == null ? e.getClass().getSimpleName() : t.getMessage();
    }
}
