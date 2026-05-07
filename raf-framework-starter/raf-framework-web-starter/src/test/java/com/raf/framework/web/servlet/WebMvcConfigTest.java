package com.raf.framework.web.servlet;

import jakarta.servlet.Filter;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Tests for WebMvcConfig.
 *
 * @author Jerry
 * @since 2026-04-27
 */
class WebMvcConfigTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebMvcConfig.class));

    @Test
    void shouldCreateWebMvcConfigBeanInServletEnvironment() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(WebMvcConfig.class));
    }

    @Test
    void shouldCreateCorsFilterWhenEnabled() {
        contextRunner
                .withPropertyValues("raf.cors.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("corsFilter");
                    assertThat(context.getBean("corsFilter")).isInstanceOf(Filter.class);
                });
    }
}
