package com.raf.framework.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "raf.ai")
public class AiProperties {

    private boolean enabled = false;

    /** 默认 Provider 名称：openai / claude / deepseek */
    private String defaultProvider = "openai";

    /** 场景路由规则，key=场景名，value=provider 名称 */
    private Map<String, String> routes = new HashMap<>();

    /** 会话管理配置 */
    private SessionConfig session = new SessionConfig();

    /** Provider 配置，key=provider 名称 */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class SessionConfig {
        /** 每个会话保留的最大消息条数 */
        private int maxHistory = 20;
        /** 会话 TTL（秒），0 表示不过期 */
        private long ttl = 3600;
    }

    @Data
    public static class ProviderConfig {
        private boolean enabled = true;
        private String apiKey;
        private String baseUrl;
        private String model;
        private double temperature = 0.7;
        private int maxTokens = 4096;
        /** 请求超时（秒） */
        private int timeout = 60;
    }
}
