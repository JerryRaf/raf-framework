package com.raf.framework.autoconfigure.rabbitmq;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
public class RabbitMqMessage implements Serializable {

    @JsonIgnore
    private static final long serialVersionUID=-1;

    private String msgId;
    private String message;
    private int times = 0;

    @JsonIgnore
    void preSend() {
        times++;
    }

    @JsonIgnore
    boolean isOverTimes() {
        return times > 3;
    }

}
