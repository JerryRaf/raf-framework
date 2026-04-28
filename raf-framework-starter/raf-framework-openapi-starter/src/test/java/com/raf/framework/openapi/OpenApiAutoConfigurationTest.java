package com.raf.framework.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Tests for OpenApiAutoConfiguration.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class OpenApiAutoConfigurationTest {

    @Test
    void shouldBuildOpenApiWhenEnabled() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.setEnabled(true);
        properties.setTitle("Test API");

        OpenApiAutoConfiguration configuration = new OpenApiAutoConfiguration();
        OpenAPI openAPI = configuration.openAPI(properties);

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Test API");
    }

    @Test
    void shouldKeepBearerAuthEnabledByDefault() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.setEnabled(true);

        OpenApiAutoConfiguration configuration = new OpenApiAutoConfiguration();
        OpenAPI openAPI = configuration.openAPI(properties);

        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }

    @Test
    void shouldSkipBearerAuthWhenDisabled() {
        OpenApiProperties properties = new OpenApiProperties();
        properties.setEnabled(true);
        properties.getBearerAuth().setEnabled(false);

        OpenApiAutoConfiguration configuration = new OpenApiAutoConfiguration();
        OpenAPI openAPI = configuration.openAPI(properties);

        assertThat(openAPI.getComponents()).isNull();
    }
}
