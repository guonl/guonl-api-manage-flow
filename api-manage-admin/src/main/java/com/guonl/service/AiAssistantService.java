package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.dao.ApiProjectDao;
import com.guonl.dao.ApiUrlDao;
import com.guonl.dao.ApiUrlDocDao;
import com.guonl.entity.ApiProject;
import com.guonl.entity.ApiUrl;
import com.guonl.entity.ApiUrlDoc;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 场景服务：多轮对话 / 调试结果分析 / 测试数据生成 / 接口文档生成 / 流程编排生成
 * <p>统一通过 {@link AiService} 调用大模型，场景差异体现在提示词与输入数据的组装。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiService aiService;
    private final ApiUrlDao apiUrlDao;
    private final ApiUrlDocDao apiUrlDocDao;
    private final ApiProjectDao apiProjectDao;

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    /** 防止提示词超长的截断保护 */
    private static final int MAX_TEXT_LEN = 8000;
    private static final int MAX_FLOW_APIS = 60;
    private static final int MAX_CHAT_HISTORY = 20;

    // ==================== 多轮对话 ====================

    public String chat(ChatRequest req) {
        List<ChatMessage> messages = req.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw new BizException("对话内容不能为空");
        }
        int from = Math.max(0, messages.size() - MAX_CHAT_HISTORY);
        List<Message> history = new ArrayList<>();
        for (ChatMessage m : messages.subList(from, messages.size())) {
            if (!StringUtils.hasText(m.getContent())) {
                continue;
            }
            if ("assistant".equalsIgnoreCase(m.getRole())) {
                history.add(new AssistantMessage(m.getContent()));
            } else {
                history.add(new UserMessage(m.getContent()));
            }
        }
        if (history.isEmpty()) {
            throw new BizException("对话内容不能为空");
        }
        return aiService.chat(history);
    }

    // ==================== 调试结果分析 ====================

    public Map<String, Object> analyze(AnalyzeRequest req) {
        if (!StringUtils.hasText(req.getResponse())) {
            throw new BizException("响应内容不能为空");
        }
        String system = """
                你是资深接口测试工程师。用户将给出一次 HTTP 调用的请求与响应信息，请分析：
                1. 响应是否符合预期（状态码、业务码、数据结构）；
                2. 若异常，给出最可能的原因（按可能性排序）；
                3. 给出排查建议（检查点、下一步动作）。
                用简体中文 Markdown 输出，小节：**结论**、**问题分析**、**排查建议**，总长不超过 400 字。
                """;
        String user = "【请求信息】\n" + clip(req.getRequest())
                + "\n\n【响应信息】\n" + clip(req.getResponse());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", aiService.call(system, user));
        return result;
    }

    // ==================== 测试数据生成 ====================

    public Map<String, Object> genData(Long urlId) {
        ApiUrl url = requireUrl(urlId);
        String system = """
                你是接口测试数据生成专家。根据接口定义生成一份测试用请求参数。
                要求：
                1. 只输出一个 JSON 对象，不要任何解释文字、不要 Markdown 代码围栏；
                2. 字段名与接口定义一致，字段类型匹配（字符串加引号、数字/布尔不加）；
                3. 值要真实合理（如中文姓名、13x 手机号、合法邮箱、业务上说得通的值）；
                4. 嵌套结构按定义保留，数组给 1-2 个示例元素。
                """;
        String user = "接口定义如下，请生成请求参数 JSON：\n" + describeApi(url);
        String reply = aiService.call(system, user);
        String dataJson = normalizeJson(reply);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataJson", dataJson);
        return result;
    }

    // ==================== 接口文档生成 ====================

    public Map<String, Object> genDoc(Long urlId) {
        ApiUrl url = requireUrl(urlId);
        ApiUrlDoc doc = apiUrlDocDao.selectByUrlId(urlId);
        String system = """
                你是 API 文档工程师。根据接口定义生成简体中文 Markdown 接口文档，包含以下小节：
                ## 基本信息（名称/方法/路径/说明）
                ## 请求参数（表格：参数名/类型/必填/说明，无参数则注明"无"）
                ## 返回说明（依据返回结构概述字段含义）
                ## 调用示例（curl 示例）
                ## 注意事项
                内容基于接口定义合理推断，不要编造业务规则。只输出 Markdown 正文。
                """;
        StringBuilder user = new StringBuilder("接口定义：\n").append(describeApi(url));
        if (doc != null && StringUtils.hasText(doc.getRemark())) {
            user.append("\n\n已有备注（可参考补充）：\n").append(clip(doc.getRemark()));
        }
        if (doc != null && StringUtils.hasText(doc.getSuccessRespExample())) {
            user.append("\n\n成功响应示例：\n").append(clip(doc.getSuccessRespExample()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", aiService.call(system, user.toString()));
        return result;
    }

    // ==================== 流程编排生成 ====================

    public Map<String, Object> genFlow(GenFlowRequest req) {
        if (req.getProjectId() == null) {
            throw new BizException("请选择项目");
        }
        if (!StringUtils.hasText(req.getRequirement())) {
            throw new BizException("请描述要编排的流程需求");
        }
        ApiProject project = apiProjectDao.selectById(req.getProjectId());
        if (project == null) {
            throw new BizException("项目不存在");
        }
        List<ApiUrl> urls = apiUrlDao.selectByProject(req.getProjectId());
        if (urls.isEmpty()) {
            throw new BizException("该项目下暂无接口，请先同步或录入接口");
        }
        String system = """
                你是接口流程编排专家。用户给出项目接口清单与流程需求，请输出流程步骤 JSON 数组。
                每个步骤的字段固定为：
                {
                  "stepName": "步骤名（简短中文）",
                  "urlId": 清单中的接口ID(数字),
                  "requestMethod": "GET/POST/PUT/DELETE",
                  "requestUrl": "接口路径",
                  "bodyType": "none|json|form-data|x-www-form-urlencoded",
                  "requestBody": "请求体JSON模板字符串，无则空串",
                  "requestHeaders": "{}",
                  "requestQuery": "{}",
                  "extractRules": [{"varName":"变量名","jsonPath":"$.data.token","desc":"说明"}],
                  "assertions": [{"type":"STATUS","target":"","expect":"200"}],
                  "onFailure": "TERMINATE 或 CONTINUE",
                  "delayMs": 0
                }
                硬性要求：
                1. 只输出 JSON 数组，不要任何解释与 Markdown 围栏；
                2. urlId 必须取自接口清单，禁止编造；
                3. 后续步骤的入参若依赖前面步骤的返回，先用 extractRules 提取变量，再在 requestBody/requestUrl 中用 {{变量名}} 引用；
                4. 断言 type 可选 STATUS/EQUALS/CONTAINS/NOT_NULL/GT/LT，target 为响应 JSONPath（STATUS 时留空）；
                5. 每个关键步骤至少带一个 STATUS=200 断言；
                6. requestBody/requestUrl 中只允许引用 extractRules 提取的变量，禁止使用未定义变量（如随机数）；需要随机值时在步骤内写死一个具体示例值。
                """;
        StringBuilder user = new StringBuilder();
        user.append("项目：").append(project.getProjectName()).append("\n\n接口清单：\n");
        int n = 0;
        for (ApiUrl u : urls) {
            if (++n > MAX_FLOW_APIS) {
                user.append("（其余接口略）\n");
                break;
            }
            user.append("urlId=").append(u.getUrlId())
                    .append(" | ").append(u.getMethodType()).append(" ").append(u.getRequestUrl())
                    .append(" | 名称: ").append(StringUtils.hasText(u.getUrlLabel()) ? u.getUrlLabel() : u.getMethodName())
                    .append(" | bodyType: ").append(StringUtils.hasText(u.getMethodContentType()) ? u.getMethodContentType() : "none")
                    .append(" | 参数定义: ").append(clip(u.getRequestParamMap(), 300))
                    .append("\n");
        }
        user.append("\n流程需求：\n").append(clip(req.getRequirement(), 2000));

        String reply = aiService.call(system, user.toString());
        List<Map<String, Object>> steps = parseSteps(reply);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("steps", steps);
        return result;
    }

    // ==================== 辅助方法 ====================

    private ApiUrl requireUrl(Long urlId) {
        if (urlId == null) {
            throw new BizException("请指定接口");
        }
        ApiUrl url = apiUrlDao.selectById(urlId);
        if (url == null) {
            throw new BizException("接口不存在");
        }
        return url;
    }

    /** 接口定义 → 文本描述（供提示词使用） */
    private String describeApi(ApiUrl u) {
        StringBuilder sb = new StringBuilder();
        sb.append("- 名称: ").append(StringUtils.hasText(u.getUrlLabel()) ? u.getUrlLabel() : u.getMethodName()).append("\n");
        sb.append("- 方法: ").append(u.getMethodType()).append(" ").append(u.getRequestUrl()).append("\n");
        sb.append("- 请求体类型: ").append(StringUtils.hasText(u.getMethodContentType()) ? u.getMethodContentType() : "none").append("\n");
        if (StringUtils.hasText(u.getRequestParamMap())) {
            sb.append("- 参数定义: ").append(clip(u.getRequestParamMap(), 1500)).append("\n");
        }
        if (StringUtils.hasText(u.getReturnParamMap())) {
            sb.append("- 返回结构: ").append(clip(u.getReturnParamMap(), 1500)).append("\n");
        }
        if (StringUtils.hasText(u.getMethodDesc())) {
            sb.append("- 说明: ").append(u.getMethodDesc()).append("\n");
        }
        return sb.toString();
    }

    /** 解析 AI 返回的流程步骤 JSON 数组 */
    private List<Map<String, Object>> parseSteps(String reply) {
        String json = extractJson(reply);
        JsonNode node;
        try {
            node = JSON_MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("流程生成结果解析失败: {}", StringUtils.hasText(reply) ? reply.substring(0, Math.min(200, reply.length())) : "");
            throw new BizException("AI 返回的流程格式无法解析，请重试或调整需求描述");
        }
        if (node == null || !node.isArray() || node.isEmpty()) {
            throw new BizException("AI 未生成有效的流程步骤，请重试");
        }
        List<Map<String, Object>> steps = new ArrayList<>();
        int order = 1;
        for (JsonNode item : node) {
            if (!item.has("urlId")) {
                continue;
            }
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("stepName", item.path("stepName").asText("步骤" + order));
            step.put("urlId", item.path("urlId").asLong());
            step.put("requestMethod", item.path("requestMethod").asText("GET"));
            step.put("requestUrl", item.path("requestUrl").asText(""));
            step.put("bodyType", item.path("bodyType").asText("none"));
            step.put("requestBody", item.path("requestBody").asText(""));
            step.put("requestHeaders", item.path("requestHeaders").asText("{}"));
            step.put("requestQuery", item.path("requestQuery").asText("{}"));
            step.put("extractRules", item.path("extractRules").toString());
            step.put("assertions", item.path("assertions").toString());
            step.put("onFailure", item.path("onFailure").asText("TERMINATE"));
            step.put("delayMs", item.path("delayMs").asInt(0));
            steps.add(step);
            order++;
        }
        if (steps.isEmpty()) {
            throw new BizException("AI 未生成有效的流程步骤，请重试");
        }
        return steps;
    }

    /** 将 AI 回复规整为合法 JSON 字符串（剥围栏、截取 JSON 主体、格式化校验） */
    private String normalizeJson(String reply) {
        String json = extractJson(reply);
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            throw new BizException("AI 返回的数据格式无法解析，请重试");
        }
    }

    /** 从文本中提取 JSON 主体：剥 Markdown 围栏、截取首个 { 或 [ 到最后的配对符 */
    private String extractJson(String text) {
        if (text == null) {
            return "";
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceAll("^```[a-zA-Z]*\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
            t = t.trim();
        }
        int brace = t.indexOf('{');
        int bracket = t.indexOf('[');
        int start;
        if (brace < 0) {
            start = bracket;
        } else if (bracket < 0) {
            start = brace;
        } else {
            start = Math.min(brace, bracket);
        }
        if (start >= 0) {
            char close = t.charAt(start) == '{' ? '}' : ']';
            int end = t.lastIndexOf(close);
            if (end > start) {
                t = t.substring(start, end + 1);
            }
        }
        return t.trim();
    }

    private String clip(String s) {
        return clip(s, MAX_TEXT_LEN);
    }

    private String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "\n...(已截断)" : s;
    }

    // ==================== 请求 DTO ====================

    @Data
    public static class ChatRequest {
        private List<ChatMessage> messages;
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }

    @Data
    public static class AnalyzeRequest {
        private String request;
        private String response;
    }

    @Data
    public static class IdRequest {
        private Long urlId;
    }

    @Data
    public static class GenFlowRequest {
        private Long projectId;
        private String requirement;
    }
}
