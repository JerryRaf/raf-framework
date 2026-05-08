package com.raf.framework.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiRequest {

    /**
     * 直接指定 Provider 名称（openai / claude / deepseek）。
     * 优先级最高，覆盖场景路由和默认 Provider。
     */
    private String provider;

    /**
     * 业务场景名称，用于触发 raf.ai.routes 中的路由规则。
     * 仅在 provider 为空时生效。
     */
    private String scene;

    /**
     * 会话 ID。不为空时启用会话管理，框架自动维护历史消息。
     */
    private String sessionId;

    /** 系统提示词（可选） */
    private String systemPrompt;

    /** 用户输入（必填） */
    private String prompt;

    /** 覆盖 Provider 默认温度（可选，null 表示使用 Provider 配置值） */
    private Double temperature;

    /** 覆盖 Provider 默认 maxTokens（可选，null 表示使用 Provider 配置值） */
    private Integer maxTokens;
}
