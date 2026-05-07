package com.raf.framework.okhttp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests Spring Boot auto-configuration registration.
 */
class OkHttpAutoConfigurationImportsTest {

    @Test
    void autoConfigurationImportsShouldOnlyContainConfigurationClasses() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");

        String imports = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(imports).contains(OkHttpAutoConfig.class.getName());
        assertThat(imports).doesNotContain(HttpExecutor.class.getName());
    }
}
