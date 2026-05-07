package io.github.jerryraf.examples.rocketmq.controller;

import com.raf.framework.autoconfigure.common.annotation.ResponseResult;
import io.github.jerryraf.examples.rocketmq.dto.OrderMsgReq;
import io.github.jerryraf.examples.rocketmq.producer.OrderProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.web.bind.annotation.*;

/**
 * RocketMQ 消息发送演示接口
 */
@RestController
@RequestMapping("/api/mq")
@ResponseResult
@RequiredArgsConstructor
public class OrderMqController {

    private final OrderProducer orderProducer;

    /** 发送普通消息 */
    @PostMapping("/normal")
    public SendResult sendNormal(@RequestBody @Valid OrderMsgReq req) {
        return orderProducer.sendNormal(req);
    }

    /** 发送顺序消息（同一 userId 严格有序） */
    @PostMapping("/fifo")
    public SendResult sendFifo(@RequestBody @Valid OrderMsgReq req) {
        return orderProducer.sendFifo(req);
    }

    /** 发送延时消息（delayLevel 3 = 10s） */
    @PostMapping("/delay")
    public SendResult sendDelay(@RequestBody @Valid OrderMsgReq req) {
        return orderProducer.sendDelay(req);
    }

    /** 发送事务消息 */
    @PostMapping("/transaction")
    public SendResult sendTransaction(@RequestBody @Valid OrderMsgReq req) {
        return orderProducer.sendTransaction(req);
    }
}
