package com.insightdevelop.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Origins allowed to call the API from a browser (bound from {@code app.cors.*}). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
