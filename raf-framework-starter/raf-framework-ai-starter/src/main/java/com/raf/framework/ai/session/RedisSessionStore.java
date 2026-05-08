package com.raf.framework.ai.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Redis 会话存储，支持集群和持久化，有 Redisson Bean 时自动激活。
 */
@Slf4j
@RequiredArgsConstructor
public class RedisSessionStore implements SessionStore {

    private static final String KEY_PREFIX = "raf:ai:session:";
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<Message> load(String sessionId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + sessionId);
            String json = bucket.get();
            if (json == null) {
                return new ArrayList<>();
            }
            List<Map<String, String>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream().map(m -> {
                String role = m.get("role");
                String content = m.get("content");
                return "user".equals(role) ? (Message) new UserMessage(content) : new AssistantMessage(content);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to load AI session from Redis, sessionId={}", sessionId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void save(String sessionId, List<Message> messages, long ttlSeconds) {
        try {
            List<Map<String, String>> raw = messages.stream()
                .map(m -> Map.of(
                    "role", m instanceof UserMessage ? "user" : "assistant",
                    "content", m.getText()
                ))
                .collect(Collectors.toList());
            String json = objectMapper.writeValueAsString(raw);
            RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + sessionId);
            if (ttlSeconds > 0) {
                bucket.set(json, Duration.ofSeconds(ttlSeconds));
            } else {
                bucket.set(json);
            }
        } catch (Exception e) {
            log.warn("Failed to save AI session to Redis, sessionId={}", sessionId, e);
        }
    }

    @Override
    public void delete(String sessionId) {
        redissonClient.getBucket(KEY_PREFIX + sessionId).delete();
    }
}
