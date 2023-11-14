package com.raf.framework.autoconfigure.feign;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;

/**
 * @author Jerry
 * @date 2019/01/01
 */
public class FeignEncoder {
    @Bean
    public Encoder feignFormEncoder() {
        return new SpringFormEncoder();
    }
}
