package com.raf.framework.mybatisplus.extension.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for MybatisPlusExtensionAutoConfiguration
 *
 * @author Jerry
 * @since 2026-04-20
 */
class MybatisPlusExtensionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusExtensionAutoConfiguration.class));

    @Test
    void testPageHelperPropertiesLoaded() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PageHelperProperties.class);
        });
    }

    @Test
    void testDefaultPageHelperProperties() {
        contextRunner.run(context -> {
            PageHelperProperties props = context.getBean(PageHelperProperties.class);
            assertThat(props.getProperties()).containsKey("helperDialect");
            assertThat(props.getProperties().get("helperDialect")).isEqualTo("mysql");
        });
    }

    @Test
    void testCustomPageHelperProperties() {
        contextRunner
                .withPropertyValues("raf.mybatis.pagehelper.properties.helperDialect=postgresql")
                .run(context -> {
                    PageHelperProperties props = context.getBean(PageHelperProperties.class);
                    assertThat(props.getProperties().get("helperDialect")).isEqualTo("postgresql");
                });
    }
}
