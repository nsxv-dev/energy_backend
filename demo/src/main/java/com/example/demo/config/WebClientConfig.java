package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.web.reactive.function.client.WebClient.builder;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient webClient() {
        return builder()
                .baseUrl("https://api.carbonintensity.org.uk")
                .build();
    }
}