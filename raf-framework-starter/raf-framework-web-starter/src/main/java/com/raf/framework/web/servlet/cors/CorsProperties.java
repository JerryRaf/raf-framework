package com.raf.framework.web.servlet.cors;

import java.util.List;
import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Data
@ConfigurationProperties(prefix = "raf.cors")
public class CorsProperties {
    private boolean enabled = false;
    private String path;
    private List<String> allowOrigins = Lists.newArrayList("*");
    private List<String> allowHeaders = Lists.newArrayList("*");
    private List<String> allowMethods = Lists.newArrayList("*");
    private List<String> allowExposeHeaders;
}
