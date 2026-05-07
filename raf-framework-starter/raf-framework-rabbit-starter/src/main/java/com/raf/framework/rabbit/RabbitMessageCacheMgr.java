package com.raf.framework.rabbit;

import com.raf.framework.core.jackson.JsonService;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
public class RabbitMessageCacheMgr {

    private Map<String, CachedMessage> now = new ConcurrentHashMap<>();

    private Map<String, CachedMessage> retry = new ConcurrentHashMap<>();

    private static volatile boolean started = false;

    private RabbitTemplate rabbitTemplate;

    private JsonService json;

    private ExecutorService executor;

    public RabbitMessageCacheMgr(RabbitTemplate rabbitTemplate, JsonService json) {
        this.rabbitTemplate = rabbitTemplate;
        this.json = json;
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("raf-rabbit-thread-pool-%d")
                .build();
        executor = new ThreadPoolExecutor(
                1, 1, 60 * 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), namedThreadFactory);
        start();
    }

    void add2Now(RabbitMqMessage rabbitMqMessage, String exchange, String routeKey) {
        now.put(
                rabbitMqMessage.getMsgId(),
                new CachedMessage(rabbitMqMessage, exchange, System.currentTimeMillis(), routeKey));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedMessage {

        private RabbitMqMessage message;

        private String exchange;

        private long timestamp;

        private String routeKey;

        /**
         * 是否因网络抖动未得到处理，5分钟
         *
         * @return
         */
        boolean isExpire() {
            return System.currentTimeMillis() - timestamp > 1000 * 60 * 5;
        }
    }

    /**
     * 将消息放到重试队列
     *
     * @param msgId
     */
    void addRetry(String msgId) {
        if (now.containsKey(msgId)) {
            retry.put(msgId, now.get(msgId));
            now.remove(msgId);
        }
    }

    /**
     * 从当前正在处理的队列中移除
     *
     * @param msgId
     */
    void remove(String msgId) {
        now.remove(msgId);
    }

    CachedMessage getNow(String msgId) {
        return now.get(msgId);
    }

    /**
     * 开始处理
     */
    private void start() {
        if (started) {
            return;
        }

        synchronized (RabbitMessageCacheMgr.class) {
            if (!started) {
                started = true;
                executor.execute(this::process);
            }
        }
    }

    private void process() {
        while (true) {
            if (retry.size() > 0) {
                Iterator<CachedMessage> iterator = retry.values().iterator();
                while (iterator.hasNext()) {
                    CachedMessage cachedMessage = iterator.next();
                    try {
                        log.info("Retrying message from retry queue: {}", json.toJson(cachedMessage.getMessage()));
                        cachedMessage.getMessage().preSend();
                        this.add2Now(
                                cachedMessage.getMessage(), cachedMessage.getExchange(), cachedMessage.getRouteKey());
                        rabbitTemplate.convertAndSend(
                                cachedMessage.getExchange(),
                                cachedMessage.getRouteKey(),
                                json.toJson(cachedMessage.getMessage()),
                                new CorrelationData(cachedMessage.getMessage().getMsgId()));
                        iterator.remove();
                    } catch (Exception ex) {
                        this.remove(cachedMessage.getMessage().getMsgId());
                        log.error("RabbitMQ retry send failed", ex);
                    }
                }
            }

            if (!now.isEmpty()) {
                Iterator<CachedMessage> iterator = now.values().iterator();
                while (iterator.hasNext()) {
                    CachedMessage cachedMessage = iterator.next();
                    try {
                        if (cachedMessage.isExpire()) {
                            log.info("Resending expired message: {}", json.toJson(cachedMessage.getMessage()));
                            cachedMessage.getMessage().preSend();
                            rabbitTemplate.convertAndSend(
                                    cachedMessage.getExchange(),
                                    cachedMessage.getRouteKey(),
                                    json.toJson(cachedMessage.getMessage()),
                                    new CorrelationData(cachedMessage.getMessage().getMsgId()));
                            iterator.remove();
                        }
                    } catch (Exception ex) {
                        log.error("RabbitMQ expired message resend failed", ex);
                    }
                }
            }

            if (retry.isEmpty() && now.isEmpty()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    log.error("RabbitMQ cache manager thread interrupted", ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
