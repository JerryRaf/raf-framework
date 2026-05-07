package io.github.jerryraf.examples.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redis Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>RedisService: String/Hash/List/Set/ZSet common operations</li>
 *   <li>Redisson distributed lock: tryLock for stock deduction</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
public class RedisExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisExampleApplication.class, args);
    }
}
