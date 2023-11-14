package com.raf.framework.autoconfigure.servlet;

import com.raf.framework.autoconfigure.servlet.http.HttpResponseInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 返回体统一拦截包装
 *
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 返回体统一拦截包装
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpResponseInterceptor()).addPathPatterns("/**");
    }
}