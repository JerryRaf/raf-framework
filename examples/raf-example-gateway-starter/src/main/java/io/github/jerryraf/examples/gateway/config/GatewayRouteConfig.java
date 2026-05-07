package io.github.jerryraf.examples.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration.
 *
 * <p>Defines routing rules for the example e-commerce backend services.
 * All routes pass through RAF Framework security filters automatically.
 *
 * @author RAF Framework Team
 * @since 2026-04-28
 */
@Configuration
public class GatewayRouteConfig {

    /**
     * Configure routes for e-commerce services.
     *
     * @param builder route locator builder
     * @return configured route locator
     */
    @Bean
    public RouteLocator ecommerceRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/users/**")
                .uri("lb://user-service"))
            .route("order-service", r -> r
                .path("/api/orders/**")
                .uri("lb://order-service"))
            .route("product-service", r -> r
                .path("/api/products/**")
                .uri("lb://product-service"))
            .build();
    }
}
