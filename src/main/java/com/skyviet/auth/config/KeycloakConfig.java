package com.skyviet.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KeycloakConfig {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Bean
    public WebClient keycloakWebClient() {
        return WebClient.builder()
                .baseUrl(serverUrl)
                .build();
    }
}
