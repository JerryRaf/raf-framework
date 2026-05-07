package io.github.jerryraf.examples.kafka.controller;

import com.raf.framework.autoconfigure.common.annotation.ResponseResult;
import com.raf.framework.kafka.KafkaMessage;
import io.github.jerryraf.examples.kafka.dto.LogEventReq;
import io.github.jerryraf.examples.kafka.producer.LogEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka 消息发送演示接口
 */
@RestController
@RequestMapping("/api/events")
@ResponseResult
@RequiredArgsConstructor
public class LogEventController {

    private final LogEventProducer logEventProducer;

    /** 同步发送日志事件（等待 Broker 确认） */
    @PostMapping("/sync")
    public KafkaMessage.SendResult sendSync(@RequestBody @Valid LogEventReq req) {
        return logEventProducer.sendSync(req);
    }

    /** 异步发送日志事件（高吞吐，不等待确认） */
    @PostMapping("/async")
    public void sendAsync(@RequestBody @Valid LogEventReq req) {
        logEventProducer.sendAsync(req);
    }
}
