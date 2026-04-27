package io.github.jerryraf.examples.mybatis.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper Scanning Configuration
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@Configuration
@MapperScan(
        basePackages = {"io.github.jerryraf.examples.mybatis.dao"},
        sqlSessionTemplateRef = "sqlSessionTemplate"
)
public class MyBatisConfig {
}
