package com.raf.framework.datasource.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dynamic DataSource Properties
 *
 * @author Jerry
 * @since 2026-04-20
 */
@Data
@ConfigurationProperties(prefix = "raf.datasource")
public class DynamicDataSourceProperties {

    /**
     * Enable multi-datasource routing
     */
    private boolean enabled = false;

    /**
     * Primary datasource name
     */
    private String primary = "master";

    /**
     * Datasource names list
     */
    private List<String> datasources = new ArrayList<>();
}
