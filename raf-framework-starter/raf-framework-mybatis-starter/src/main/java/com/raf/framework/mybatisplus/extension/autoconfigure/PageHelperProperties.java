package com.raf.framework.mybatisplus.extension.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PageHelper configuration properties
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Data
@ConfigurationProperties(prefix = "raf.mybatis.pagehelper")
public class PageHelperProperties {

    /**
     * PageHelper plugin properties map
     */
    private Map<String, String> properties = new HashMap<>();

    public PageHelperProperties() {
        properties.put("helperDialect", "mysql");
        properties.put("reasonable", "true");
        properties.put("supportMethodsArguments", "true");
        properties.put("params", "count=countSql");
    }
}
