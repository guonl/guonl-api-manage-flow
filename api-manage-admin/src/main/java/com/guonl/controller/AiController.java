package com.guonl.controller;

import com.guonl.common.ApiResult;
import com.guonl.service.AiAssistantService;
import com.guonl.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 助手：状态/连通测试 + 五大场景（对话、结果分析、测试数据、文档、流程编排）
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiAssistantService aiAssistantService;

    /**
     * AI 配置状态（是否已配置、模型名等）
     */
    @GetMapping("/status")
    public ApiResult<Map<String, Object>> status() {
        return ApiResult.ok(aiService.status());
    }

    /**
     * 连通性测试
     */
    @PostMapping("/test")
    public ApiResult<Map<String, Object>> test() {
        return ApiResult.ok(aiService.test());
    }

    /**
     * 多轮对话
     */
    @PostMapping("/chat")
    public ApiResult<String> chat(@RequestBody AiAssistantService.ChatRequest req) {
        return ApiResult.ok(aiAssistantService.chat(req));
    }

    /**
     * 调试结果分析
     */
    @PostMapping("/analyze")
    public ApiResult<Map<String, Object>> analyze(@RequestBody AiAssistantService.AnalyzeRequest req) {
        return ApiResult.ok(aiAssistantService.analyze(req));
    }

    /**
     * 生成接口测试数据
     */
    @PostMapping("/gen-data")
    public ApiResult<Map<String, Object>> genData(@RequestBody AiAssistantService.IdRequest req) {
        return ApiResult.ok(aiAssistantService.genData(req.getUrlId()));
    }

    /**
     * 生成接口文档（Markdown）
     */
    @PostMapping("/gen-doc")
    public ApiResult<Map<String, Object>> genDoc(@RequestBody AiAssistantService.IdRequest req) {
        return ApiResult.ok(aiAssistantService.genDoc(req.getUrlId()));
    }

    /**
     * 根据需求描述生成流程步骤 JSON
     */
    @PostMapping("/gen-flow")
    public ApiResult<Map<String, Object>> genFlow(@RequestBody AiAssistantService.GenFlowRequest req) {
        return ApiResult.ok(aiAssistantService.genFlow(req));
    }
}
