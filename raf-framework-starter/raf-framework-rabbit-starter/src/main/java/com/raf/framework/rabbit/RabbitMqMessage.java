package com.raf.framework.rabbit;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
public class RabbitMqMessage implements Serializable {

    @JsonIgnore
    private static final long serialVersionUID = -1;

    private String msgId;
    private String message;
    private int times = 0;

    @JsonIgnore
    public void incrementRetryCount() {
        times++;
    }

    /**
     * 是否超过最大重试次数（默认 3 次）。
     * 在 {@link AbstractRabbitConsumerListener#retry} 中使用，防止无限重试。
     */
    @JsonIgnore
    public boolean isOverTimes() {
        return times > 3;
    }
}
