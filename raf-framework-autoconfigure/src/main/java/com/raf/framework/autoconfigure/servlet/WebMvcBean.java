package com.raf.framework.autoconfigure.servlet;

import com.raf.framework.autoconfigure.servlet.cors.CorsProperties;
import com.raf.framework.autoconfigure.servlet.log.AccessLogFilter;
import com.raf.framework.autoconfigure.servlet.log.AuditProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import java.util.Optional;

/**
 * 跨域配置，日志配置
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({CorsProperties.class, AuditProperties.class})
public class WebMvcBean {

    @Bean
    @ConditionalOnClass(CorsFilter.class)
    @ConditionalOnProperty(value = "raf.cors.enabled")
    public Filter corsFilter(CorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(
                Optional.ofNullable(corsProperties.getPath()).orElse("/**"),
                buildConfig(corsProperties));
        return new CorsFilter(source);
    }

    @Bean
    @ConditionalOnClass(OncePerRequestFilter.class)
    @ConditionalOnProperty(value = "raf.log.enabled")
    public Filter accessLogFilter() {
        return new AccessLogFilter();
    }

    private CorsConfiguration buildConfig(CorsProperties corsProperties) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        Optional.ofNullable(corsProperties.getAllowOrigins())
                .ifPresent(origins -> origins.forEach(corsConfiguration::addAllowedOrigin));
        Optional.ofNullable(corsProperties.getAllowHeaders())
                .ifPresent(headers -> headers.forEach(corsConfiguration::addAllowedHeader));
        Optional.ofNullable(corsProperties.getAllowMethods())
                .ifPresent(methods -> methods.forEach(corsConfiguration::addAllowedMethod));
        Optional.ofNullable(corsProperties.getAllowExposeHeaders())
                .ifPresent(headers -> headers.forEach(corsConfiguration::addExposedHeader));
        return corsConfiguration;
    }
}