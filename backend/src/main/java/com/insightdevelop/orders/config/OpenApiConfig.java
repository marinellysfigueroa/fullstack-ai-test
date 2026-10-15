package com.insightdevelop.orders.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. Swagger UI: /swagger-ui.html, spec: /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expeditedOrdersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Expedited Orders API")
                .version("0.1.0")
                .description("""
                        Evaluates customer orders for expedited processing and approves eligible ones.

                        Eligibility is recomputed on every read because some rules depend on time
                        (inventory freshness, order age). Hard rules block the order regardless of score;
                        the score starts at 100 and must stay >= 60."""));
    }
}
