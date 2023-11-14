package com.raf.framework.autoconfigure.rabbitmq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.raf.framework.autoconfigure.jackson.Json;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

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

    private Json json;

    private ExecutorService executor;

    public RabbitMessageCacheMgr(RabbitTemplate rabbitTemplate, Json json) {
        this.rabbitTemplate = rabbitTemplate;
        this.json = json;
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("raf-rabbit-thread-pool-%d").build();
        executor = new ThreadPoolExecutor(2, 5, 200L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10), namedThreadFactory);
        start();
    }

    void add2Now(RabbitMqMessage rabbitMqMessage, String exchange, String routeKey) {
        now.put(rabbitMqMessage.getMsgId(), new CachedMessage(rabbitMqMessage, exchange, System.currentTimeMillis(), routeKey));
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
            /** 重试队列处理*/
            if (retry.size() > 0) {
                Iterator<CachedMessage> iterator = retry.values().iterator();
                while (iterator.hasNext()) {
                    CachedMessage cachedMessage = iterator.next();
                    try {
                        log.info("重试队列中重试:" + json.objToString(cachedMessage.getMessage()));
                        cachedMessage.getMessage().preSend();
                        this.add2Now(cachedMessage.getMessage(), cachedMessage.getExchange(), cachedMessage.getRouteKey());
                        rabbitTemplate.convertAndSend(cachedMessage.getExchange(), cachedMessage.getRouteKey(), json.objToString(cachedMessage.getMessage()), new CorrelationData(cachedMessage.getMessage().getMsgId()));
                        iterator.remove();
                    } catch (Exception ex) {
                        this.remove(cachedMessage.getMessage().getMsgId());
                        log.error("Mq重发消息失败", ex);
                    }
                }
            }

            if (!now.isEmpty()) {
                Iterator<CachedMessage> iterator = now.values().iterator();
                while (iterator.hasNext()) {
                    CachedMessage cachedMessage = iterator.next();
                    try {
                        /** 过期了重新发送*/
                        if (cachedMessage.isExpire()) {
                            log.info("超时队列中处理: " + json.objToString(cachedMessage.getMessage()));
                            cachedMessage.getMessage().preSend();
                            rabbitTemplate.convertAndSend(cachedMessage.getExchange(), cachedMessage.getRouteKey(), json.objToString(cachedMessage.getMessage()), new CorrelationData(cachedMessage.getMessage().getMsgId()));
                            iterator.remove();
                        }
                    } catch (Exception ex) {
                        log.error("Mq过期消息发送失败", ex);
                    }
                }
            }

            /** 当前队列依然存在超时的数据需要处理*/
            if (retry.isEmpty() && now.isEmpty()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ex) {
                    log.error("Mq队列中断异常", ex);
                }
            }
        }
    }

}
