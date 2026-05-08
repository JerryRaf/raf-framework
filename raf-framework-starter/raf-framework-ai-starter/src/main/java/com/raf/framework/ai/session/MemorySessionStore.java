package com.raf.framework.ai.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.ai.chat.messages.Message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 内存会话存储，基于 Caffeine Cache 实现 TTL 自动过期。
 * 重启后丢失，适合开发/单机场景。
 */
public class MemorySessionStore implements SessionStore {

    /**
     * 默认最大会话数，防止无限增长。
     * TTL 在首次 save 时通过 Caffeine 的 expireAfterWrite 设置。
     * 由于 Caffeine 不支持 per-entry TTL，使用固定 TTL（首次 save 时的 ttlSeconds）。
     * 若 ttlSeconds=0，则使用 24 小时作为默认过期时间。
     */
    private static final int MAX_SIZE = 10_000;
    private static final long DEFAULT_TTL_SECONDS = 86400L; // 24h

    private final Cache<String, List<Message>> cache;

    public MemorySessionStore() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterWrite(DEFAULT_TTL_SECONDS, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public List<Message> load(String sessionId) {
        List<Message> messages = cache.getIfPresent(sessionId);
        return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
    }

    @Override
    public void save(String sessionId, List<Message> messages, long ttlSeconds) {
        cache.put(sessionId, new ArrayList<>(messages));
    }

    @Override
    public void delete(String sessionId) {
        cache.invalidate(sessionId);
    }
}

