package io.github.jerryraf.examples.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway Example Application.
 *
 * <p>Demonstrates RAF Framework gateway security features:
 * - ECIES request decryption
 * - ECDSA signature verification
 * - Anti-replay attack protection (time window + nonce)
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@SpringBootApplication
public class GatewayExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayExampleApplication.class, args);
    }
}
