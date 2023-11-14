package com.raf.framework.autoconfigure.okhttp;


import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnProperty(value = "raf.okhttp.enabled")
@ConditionalOnClass(HttpLoggingInterceptor.class)
@AutoConfigureBefore(OkHttpAutoConfig.class)
@EnableConfigurationProperties(OkHttpClientProperties.class)
public class OkHttpLoggingAutoConfig {

    @Autowired
    private OkHttpClientProperties loggingProperties;

    @Autowired(required = false)
    private HttpLoggingInterceptor.Logger logger;

    @Bean
    @ConditionalOnMissingBean
    public HttpLoggingInterceptor okHttp3LoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor;
        if (logger != null) {
            httpLoggingInterceptor = new HttpLoggingInterceptor(logger);
        } else {
            httpLoggingInterceptor = new HttpLoggingInterceptor();
        }

        HttpLoggingInterceptor.Level level = loggingProperties.getLevel();
        if (level != null) {
            httpLoggingInterceptor.setLevel(level);
        }

        return httpLoggingInterceptor;
    }
}
