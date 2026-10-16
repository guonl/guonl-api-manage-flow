package com.guonl.service;

import com.guonl.common.BizException;
import com.guonl.common.Constants;
import com.guonl.dao.SysConfigDao;
import com.guonl.entity.SysConfig;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 服务：基于 sys_config 动态构建 OpenAI 兼容协议 ChatClient。
 * <p>配置变更后通过 {@link #evictClient()} 失效缓存，下次调用自动重建。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    /** 连接超时（秒） */
    private static final Duration CLIENT_TIMEOUT = Duration.ofSeconds(60);

    private final SysConfigDao sysConfigDao;

    /** 缓存的 ChatClient 及其构建时的配置指纹 */
    private volatile ChatClient cachedClient;
    private volatile String fingerprint = "";

    // ==================== 状态与连通性 ====================

    /**
     * AI 配置状态
     */
    public Map<String, Object> status() {
        String baseUrl = cfg(Constants.CFG_AI_BASE_URL);
        String model = cfg(Constants.CFG_AI_MODEL);
        String temperature = cfg(Constants.CFG_AI_TEMPERATURE);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("configured", StringUtils.hasText(baseUrl) && StringUtils.hasText(cfg(Constants.CFG_AI_API_KEY)) && StringUtils.hasText(model));
        map.put("baseUrl", baseUrl);
        map.put("model", model);
        map.put("temperature", temperature);
        map.put("ready", cachedClient != null);
        return map;
    }

    /**
     * 连通性测试：发送一句简单问候，返回模型回复与耗时
     */
    public Map<String, Object> test() {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            String reply = client().prompt().user("请只回复两个字：pong").call().content();
            result.put("success", true);
            result.put("reply", reply);
            result.put("latencyMs", System.currentTimeMillis() - start);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 连通性测试失败: {}", e.getMessage());
            result.put("success", false);
            result.put("reply", e.getMessage());
            result.put("latencyMs", System.currentTimeMillis() - start);
        }
        return result;
    }

    // ==================== 通用调用 ====================

    /**
     * 单轮调用：system + user → 文本回复
     */
    public String call(String systemPrompt, String userPrompt) {
        return client().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 多轮对话：历史消息（role: user/assistant）→ 文本回复
     */
    public String chat(List<Message> history) {
        return client().prompt()
                .system(DEFAULT_CHAT_SYSTEM)
                .messages(history)
                .call()
                .content();
    }

    /** 对话场景默认人设 */
    private static final String DEFAULT_CHAT_SYSTEM = """
            你是「API 管理平台」的智能助手，精通 REST API 设计、接口测试、流程编排。
            回答使用简体中文，简洁专业；涉及 JSON/代码时用 Markdown 代码块。
            """;

    // ==================== ChatClient 管理 ====================

    /**
     * 获取 ChatClient（懒加载 + 配置指纹缓存）
     */
    public ChatClient client() {
        String baseUrl = cfg(Constants.CFG_AI_BASE_URL);
        String apiKey = cfg(Constants.CFG_AI_API_KEY);
        String model = cfg(Constants.CFG_AI_MODEL);
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(model)) {
            throw new BizException("AI 尚未配置，请先在「系统设置」中填写 Base URL、API Key 和模型名称");
        }
        String fp = fingerprint(baseUrl, apiKey, model, cfg(Constants.CFG_AI_TEMPERATURE));
        ChatClient client = cachedClient;
        if (client == null || !fp.equals(fingerprint)) {
            synchronized (this) {
                if (cachedClient == null || !fp.equals(fingerprint)) {
                    cachedClient = buildClient(baseUrl, apiKey, model, cfg(Constants.CFG_AI_TEMPERATURE));
                    fingerprint = fp;
                    client = cachedClient;
                    log.info("AI ChatClient 已重建: baseUrl={}, model={}", baseUrl, model);
                }
            }
        }
        return cachedClient;
    }

    /**
     * 配置变更后使缓存失效
     */
    public synchronized void evictClient() {
        cachedClient = null;
        fingerprint = "";
    }

    private ChatClient buildClient(String baseUrl, String apiKey, String model, String temperature) {
        // Spring AI 2.0.1：OpenAI 官方 SDK(4.49.0) + Spring 自带 OkHttp 传输层
        // 注意：必须同时提供同步与异步客户端（流式输出使用 async），否则 build() 会
        // 用空配置自行构建 async 客户端，导致 "At least one credential source" 异常
        OpenAIClient openAiClient = new OpenAIClientImpl(ClientOptions.builder()
                .httpClient(SpringAiOpenAiHttpClient.builder().timeout(CLIENT_TIMEOUT).build())
                .baseUrl(baseUrl.trim())
                .apiKey(apiKey.trim())
                .build());
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder().model(model.trim());
        if (StringUtils.hasText(temperature)) {
            try {
                optionsBuilder.temperature(Double.parseDouble(temperature.trim()));
            } catch (NumberFormatException ignored) {
                // 温度配置非法时使用模型默认值
            }
        }
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .openAiClientAsync(openAiClient.async())
                .options(optionsBuilder.build())
                .build();
        return ChatClient.builder(chatModel).build();
    }

    private String fingerprint(String baseUrl, String apiKey, String model, String temperature) {
        return String.join("|", baseUrl, Integer.toHexString(apiKey.hashCode()), model, temperature);
    }

    private String cfg(String key) {
        SysConfig config = sysConfigDao.selectByKey(key);
        return config == null ? null : config.getConfigValue();
    }
}
