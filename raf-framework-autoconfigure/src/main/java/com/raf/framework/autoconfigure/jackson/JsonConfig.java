package com.raf.framework.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnBean(ObjectMapper.class)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration")
public class JsonConfig {

    @Bean
    public Json json() {
        return new Json();
    }

    /**
     * 返回Long转String,解决js精度问题
     * @return
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
