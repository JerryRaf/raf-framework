package io.github.jerryraf.examples.kms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * KMS Starter Example Application.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>LOCAL mode: load plaintext keys from config, no cloud account needed</li>
 *   <li>CryptoManager: encrypt/decrypt/generateIndex via alias</li>
 * </ul>
 *
 * @author Jerry
 */
@SpringBootApplication
public class KmsExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(KmsExampleApplication.class, args);
    }
}
