package com.raf.example.ai.controller;

import com.raf.framework.ai.AiRequest;
import com.raf.framework.ai.AiService;
import com.raf.framework.core.common.result.RafResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * 同步调用（走默认 Provider）
     * GET /ai/chat?prompt=你好
     */
    @GetMapping("/chat")
    public RafResult<String> chat(@RequestParam String prompt) {
        return RafResult.ok(aiService.chat(prompt));
    }

    /**
     * 指定 Provider 同步调用
     * GET /ai/chat/openai?prompt=你好
     */
    @GetMapping("/chat/{provider}")
    public RafResult<String> chatWithProvider(
            @PathVariable String provider,
            @RequestParam String prompt) {
        return RafResult.ok(aiService.chat(provider, prompt));
    }

    /**
     * 流式调用（SSE），走默认 Provider
     * GET /ai/stream?prompt=你好
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String prompt) {
        return aiService.streamChat(prompt);
    }

    /**
     * 带会话上下文的多轮对话（流式）
     * GET /ai/session?sessionId=user-123&prompt=你好
     */
    @GetMapping(value = "/session", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sessionChat(
            @RequestParam String sessionId,
            @RequestParam String prompt) {
        AiRequest request = AiRequest.builder()
            .sessionId(sessionId)
            .prompt(prompt)
            .build();
        return aiService.streamChat(request);
    }

    /**
     * 完整参数调用示例（同步）
     * POST /ai/full
     */
    @PostMapping("/full")
    public RafResult<String> fullChat(@RequestBody AiRequest request) {
        return RafResult.ok(aiService.chat(request));
    }
}
