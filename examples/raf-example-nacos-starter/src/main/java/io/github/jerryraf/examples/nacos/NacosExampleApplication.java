package io.github.jerryraf.examples.nacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Nacos Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Nacos Config: dynamic property refresh via @RefreshScope</li>
 *   <li>Nacos Discovery: service registration and discovery</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NacosExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosExampleApplication.class, args);
    }
}
