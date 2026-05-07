package io.github.jerryraf.examples.openapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rafExampleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAF Framework Example API")
                        .description("Demonstrates Springdoc OpenAPI 3 integration with raf-framework-web-starter")
                        .version("1.0.0")
                        .contact(new Contact().name("Jerry").url("https://github.com/JerryRaf"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
