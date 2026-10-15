package com.insightdevelop.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExpeditedOrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpeditedOrdersApplication.class, args);
    }
}
