package com.raf.framework.mybatisplus.extension.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration for MyBatis-Plus extension
 *
 * @author Jerry
 * @since 2026-04-20
 */
@AutoConfiguration
@EnableConfigurationProperties(PageHelperProperties.class)
public class MybatisPlusExtensionAutoConfiguration {
}
