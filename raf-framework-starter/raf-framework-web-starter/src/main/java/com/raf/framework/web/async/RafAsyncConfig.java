package com.raf.framework.web.async;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 异步线程池自动配置
 * <p>
 * 只有当配置文件中存在 raf.async.enabled=true 时，此配置才生效。
 * matchIfMissing = false 表示如果不写这个配置，默认不生效。
 *
 * @author Jerry
 */
@Configuration
@EnableConfigurationProperties(RafAsyncProperties.class)
@ConditionalOnProperty(prefix = "raf.async", name = "enabled", havingValue = "true")
@Import(RafAsyncBeanRegistrar.class)
public class RafAsyncConfig {

}