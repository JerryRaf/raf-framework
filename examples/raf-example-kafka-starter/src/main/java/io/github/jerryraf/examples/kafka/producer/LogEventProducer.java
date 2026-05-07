package io.github.jerryraf.examples.kafka.producer;

import com.raf.framework.kafka.KafkaMessage;
import com.raf.framework.kafka.KafkaProducer;
import io.github.jerryraf.examples.kafka.dto.LogEventReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志事件生产者
 * 演示：同步发送、异步发送、批量发送
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogEventProducer {

    private static final String TOPIC_LOG_EVENTS = "log-events";
    private static final String TOPIC_LOG_BATCH  = "log-batch";

    private final KafkaProducer kafkaProducer;

    /**
     * 同步发送（等待 Broker 确认）
     */
    public KafkaMessage.SendResult sendSync(LogEventReq req) {
        KafkaMessage.SendResult result = kafkaProducer.sendSync(
            TOPIC_LOG_EVENTS,
            req.getServiceName(),   // key：按服务名路由到固定分区，保证同一服务日志有序
            req
        );
        log.info("日志事件同步发送成功，service={}, partition={}, offset={}",
            req.getServiceName(), result.getPartition(), result.getOffset());
        return result;
    }

    /**
     * 异步发送（高吞吐场景）
     */
    public void sendAsync(LogEventReq req) {
        KafkaMessage<LogEventReq> message = KafkaMessage.<LogEventReq>builder()
            .topic(TOPIC_LOG_EVENTS)
            .key(req.getServiceName())
            .body(req)
            .build();

        kafkaProducer.sendAsync(message, (metadata, exception) -> {
            if (exception != null) {
                log.error("日志事件异步发送失败，service={}", req.getServiceName(), exception);
            } else {
                log.debug("日志事件异步发送成功，partition={}, offset={}",
                    metadata.partition(), metadata.offset());
            }
        });
    }
}
