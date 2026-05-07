package io.github.jerryraf.examples.rabbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Normal message: Direct Exchange, manual ACK, idempotency (Redis msgId dedup), exception handling</li>
 *   <li>Delay queue: DLX + TTL pattern for order timeout cancellation</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
public class RabbitExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RabbitExampleApplication.class, args);
    }
}
