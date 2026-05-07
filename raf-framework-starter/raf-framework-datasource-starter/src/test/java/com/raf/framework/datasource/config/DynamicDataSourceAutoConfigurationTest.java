package com.raf.framework.datasource.config;

import com.raf.framework.datasource.DynamicRoutingDataSource;

import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * DynamicDataSourceAutoConfiguration Test
 *
 * @author Jerry
 * @since 2026-04-20
 */
class DynamicDataSourceAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicDataSourceAutoConfiguration.class));

    @Test
    void testAutoConfigurationDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DynamicRoutingDataSource.class);
        });
    }

    @Test
    void testAutoConfigurationEnabledWithProperty() {
        contextRunner
                .withUserConfiguration(TestDataSourceConfiguration.class)
                .withPropertyValues(
                        "raf.datasource.enabled=true",
                        "raf.datasource.primary=master",
                        "raf.datasource.datasources[0]=master",
                        "raf.datasource.datasources[1]=slave"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicRoutingDataSource.class);
                    assertThat(context).hasSingleBean(DynamicDataSourceProperties.class);
                });
    }

    @Configuration
    static class TestDataSourceConfiguration {

        @Bean
        public DataSource master() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("master")
                    .build();
        }

        @Bean
        public DataSource slave() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("slave")
                    .build();
        }
    }
}
