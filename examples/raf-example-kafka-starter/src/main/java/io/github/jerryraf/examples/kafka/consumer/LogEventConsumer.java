package io.github.jerryraf.examples.kafka.consumer;

import com.raf.framework.kafka.AbstractKafkaConsumerListener;
import com.raf.framework.kafka.KafkaConsumer;
import io.github.jerryraf.examples.kafka.dto.LogEventReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

/**
 * 日志事件消费者（手动 ACK，MANUAL_IMMEDIATE 模式）
 */
@KafkaConsumer(
    topics = {"log-events"},
    groupId = "log-consumer-group",
    concurrency = 3,
    ackMode = "MANUAL_IMMEDIATE"
)
@Slf4j
public class LogEventConsumer extends AbstractKafkaConsumerListener<LogEventReq> {

    @Override
    public void onMessage(ConsumerRecord<String, String> record, LogEventReq event, Acknowledgment ack) {
        try {
            log.info("消费日志事件，service={}, level={}, partition={}, offset={}",
                event.getServiceName(), event.getLevel(),
                record.partition(), record.offset());

            processLogEvent(event);

            // 处理成功后手动提交 offset
            ack.acknowledge();
        } catch (Exception e) {
            log.error("消费日志事件失败，key={}, offset={}", record.key(), record.offset(), e);
            // 不 ack，触发重试（需配合重试机制）
        }
    }

    private void processLogEvent(LogEventReq event) {
        // 实际场景：写入 Elasticsearch、ClickHouse 等存储
        log.info("处理日志事件：[{}] {} - {}", event.getLevel(), event.getServiceName(), event.getMessage());
    }
}
