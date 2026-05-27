package com.skyviet.auth.service;

import com.skyviet.auth.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final WebClient keycloakWebClient;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    public AuthResponse login(LoginRequest request) {
        try {
            Map<String, Object> response = keycloakWebClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "password")
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("username", request.email())
                            .with("password", request.password()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Keycloak login response: {}", response);

            return toAuthResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("Keycloak error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public AuthResponse refresh(RefreshRequest request) {
        Map<String, Object> response = keycloakWebClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", request.refreshToken()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return toAuthResponse(response);
    }

    public void logout(String refreshToken) {
        keycloakWebClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @SuppressWarnings("unchecked")
    public void register(RegisterRequest request) {
        String adminToken = getAdminToken();

        Map<String, Object> userPayload = Map.of(
                "email", request.email(),
                "username", request.email(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false
                ))
        );

        try {
            keycloakWebClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(userPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException.Conflict e) {
            throw new IllegalArgumentException("Email already registered");
        }
    }

    @SuppressWarnings("unchecked")
    private String getAdminToken() {
        Map<String, Object> response = keycloakWebClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "admin-cli")
                        .with("username", adminUsername)
                        .with("password", adminPassword))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private AuthResponse toAuthResponse(Map<String, Object> response) {
        return new AuthResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                ((Number) response.get("expires_in")).longValue(),
                (String) response.get("token_type")
        );
    }
}
