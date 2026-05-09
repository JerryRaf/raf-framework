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

    private static final long serialVersionUID = 1L;

    private String msgId;
    private String message;
    private int times = 0;

    @JsonIgnore
    public void incrementRetryCount() {
        times++;
    }
}
