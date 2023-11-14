package com.raf.framework.autoconfigure.servlet.version;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * @author Jerry
 * @date 2019/01/01
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcRegistrationsConfig implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        ApiVersionHandlerMapping apiVersionHandlerMapping = new ApiVersionHandlerMapping();
        apiVersionHandlerMapping.setOrder(0);
        return apiVersionHandlerMapping;
    }

}