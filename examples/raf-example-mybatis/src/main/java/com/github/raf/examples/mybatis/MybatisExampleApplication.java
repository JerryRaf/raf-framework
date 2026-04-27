package io.github.jerryraf.examples.mybatis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * RAF Framework MyBatis Example Application
 *
 * This example demonstrates:
 * 1. Multi-datasource configuration
 * 2. Master-slave read-write splitting
 * 3. MyBatis-Plus pagination
 * 4. Transaction management
 *
 * @author RAF Framework Team
 * @since 2026-04-20
 */
@SpringBootApplication
@EnableTransactionManagement(proxyTargetClass = true)
public class MybatisExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisExampleApplication.class, args);
    }
}
