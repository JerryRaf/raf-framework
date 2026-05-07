package io.github.jerryraf.examples.rabbit.service;

import com.raf.framework.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Message idempotency service using Redis setIfAbsent.
 *
 * <p>Pattern: before processing, try to set a key with the msgId.
 * If the key already exists, the message was already processed — skip it.
 * TTL is set to 24h to cover any reasonable redelivery window.
 *
 * @author Jerry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentService {

    private static final String IDEMPOTENT_KEY_PREFIX = "mq:idempotent:";
    private static final long IDEMPOTENT_TTL_HOURS = 24;

    private final RedisService redisService;

    /**
     * Try to mark a message as being processed.
     *
     * @param msgId unique message ID
     * @return true if this is the first time processing (proceed), false if duplicate (skip)
     */
    public boolean tryMark(String msgId) {
        String key = IDEMPOTENT_KEY_PREFIX + msgId;
        boolean marked = redisService.setIfAbsent(key, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (!marked) {
            log.warn("Duplicate message detected, skipping: msgId={}", msgId);
        }
        return marked;
    }
}
