package com.raf.framework.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raf.framework.ai.provider.AiProvider;
import com.raf.framework.ai.provider.ClaudeProvider;
import com.raf.framework.ai.provider.DeepSeekProvider;
import com.raf.framework.ai.provider.OpenAiProvider;
import com.raf.framework.ai.provider.ProviderFactory;
import com.raf.framework.ai.router.AiRouter;
import com.raf.framework.ai.session.MemorySessionStore;
import com.raf.framework.ai.session.RedisSessionStore;
import com.raf.framework.ai.session.SessionManager;
import com.raf.framework.ai.session.SessionStore;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "raf.ai", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AiProperties.class)
public class AiAutoConfiguration {

    @Autowired
    private AiProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public SessionStore sessionStore(
            @Autowired(required = false) RedissonClient redissonClient,
            @Autowired(required = false) ObjectMapper objectMapper) {
        if (redissonClient != null) {
            log.info("-->AiAutoConfiguration using RedisSessionStore for AI session management");
            ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
            return new RedisSessionStore(redissonClient, mapper);
        }
        log.info("-->AiAutoConfiguration using MemorySessionStore for AI session management");
        return new MemorySessionStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(SessionStore sessionStore) {
        return new SessionManager(sessionStore, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiRouter aiRouter() {
        return new AiRouter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProviderFactory providerFactory() {
        Map<String, AiProvider> providerMap = new HashMap<>();
        properties.getProviders().forEach((name, config) -> {
            if (!config.isEnabled()) {
                return;
            }
            AiProvider provider = switch (name.toLowerCase()) {
                case "openai" -> new OpenAiProvider(config);
                case "claude" -> new ClaudeProvider(config);
                case "deepseek" -> new DeepSeekProvider(config);
                default -> {
                    log.warn("-->AiAutoConfiguration unknown provider: {}, skipping", name);
                    yield null;
                }
            };
            if (provider != null) {
                providerMap.put(name, provider);
                log.info("-->AiAutoConfiguration registered AI provider: {}", name);
            }
        });
        return new ProviderFactory(providerMap);
    }

    @Bean
    @ConditionalOnMissingBean
    public AiService aiService(ProviderFactory providerFactory, AiRouter aiRouter, SessionManager sessionManager) {
        return new AiServiceImpl(providerFactory, aiRouter, sessionManager);
    }
}
