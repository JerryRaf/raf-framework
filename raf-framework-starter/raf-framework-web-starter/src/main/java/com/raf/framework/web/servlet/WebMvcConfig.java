package com.raf.framework.web.servlet;

import com.raf.framework.web.servlet.cors.CorsProperties;
import com.raf.framework.web.servlet.http.HttpResponseInterceptor;
import com.raf.framework.web.servlet.log.AccessLogFilter;
import com.raf.framework.web.servlet.log.AuditProperties;

import java.util.Optional;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration: registers interceptors, CORS filter, and access log filter.
 *
 * @author Jerry
 * @since 2019-01-01
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({CorsProperties.class, AuditProperties.class})
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Register HTTP interceptors.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpResponseInterceptor()).addPathPatterns("/**");
    }

    @Bean
    @ConditionalOnClass(CorsFilter.class)
    @ConditionalOnProperty(value = "raf.cors.enabled", havingValue = "true")
    public Filter corsFilter(CorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(
                Optional.ofNullable(corsProperties.getPath()).orElse("/**"), buildConfig(corsProperties));
        return new CorsFilter(source);
    }

    @Bean
    @ConditionalOnClass(OncePerRequestFilter.class)
    @ConditionalOnProperty(value = "raf.log.enabled", havingValue = "true")
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