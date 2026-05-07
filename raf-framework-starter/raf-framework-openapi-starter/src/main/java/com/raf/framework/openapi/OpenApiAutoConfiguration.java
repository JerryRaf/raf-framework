package com.raf.framework.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 自动配置
 *
 * @author Jerry
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "raf.openapi", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenApiProperties.class)
public class OpenApiAutoConfiguration {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI(OpenApiProperties properties) {
        Info info = new Info()
                .title(properties.getTitle())
                .description(properties.getDescription())
                .version(properties.getVersion())
                .contact(new Contact()
                        .name(properties.getContact().getName())
                        .email(properties.getContact().getEmail())
                        .url(properties.getContact().getUrl()))
                .license(new License()
                        .name(properties.getLicense().getName())
                        .url(properties.getLicense().getUrl()));

        OpenAPI openAPI = new OpenAPI().info(info);

        if (properties.getBearerAuth().isEnabled()) {
            openAPI.components(new Components()
                    .addSecuritySchemes(BEARER_AUTH_SCHEME,
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .in(SecurityScheme.In.HEADER)
                                    .name(properties.getBearerAuth().getHeaderName())))
                    .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
        }

        return openAPI;
    }
}
