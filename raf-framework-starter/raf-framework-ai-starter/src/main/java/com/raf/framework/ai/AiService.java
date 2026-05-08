package com.raf.framework.ai;

import reactor.core.publisher.Flux;

/**
 * AI 大模型统一门面接口。
 * 业务层注入此接口即可调用 OpenAI、Claude、DeepSeek。
 */
public interface AiService {

    /**
     * 同步调用，走默认 Provider。
     *
     * @param prompt 用户输入
     * @return AI 响应文本
     */
    String chat(String prompt);

    /**
     * 同步调用，指定 Provider。
     *
     * @param provider provider 名称（openai / claude / deepseek）
     * @param prompt   用户输入
     * @return AI 响应文本
     */
    String chat(String provider, String prompt);

    /**
     * 同步调用，完整参数。
     *
     * @param request 请求对象（支持 provider、scene、sessionId、systemPrompt 等）
     * @return AI 响应文本
     */
    String chat(AiRequest request);

    /**
     * 流式调用，走默认 Provider。
     *
     * @param prompt 用户输入
     * @return token 流
     */
    Flux<String> streamChat(String prompt);

    /**
     * 流式调用，完整参数。
     *
     * @param request 请求对象
     * @return token 流
     */
    Flux<String> streamChat(AiRequest request);
}
