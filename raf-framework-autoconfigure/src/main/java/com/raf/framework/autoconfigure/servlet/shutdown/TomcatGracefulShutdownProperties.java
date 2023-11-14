package com.raf.framework.autoconfigure.servlet.shutdown;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties(prefix = "raf.tomcat.shutdown")
public class TomcatGracefulShutdownProperties {

    private boolean enabled = false;
    /**
     * 单位秒
     */
    private Integer waitTime = 30;
}
