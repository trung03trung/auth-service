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
        try {
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

            log.info("Keycloak refresh response: {}", response);
            return toAuthResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("Keycloak refresh error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    public void logout(String refreshToken) {
        try {
            keycloakWebClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("refresh_token", refreshToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Keycloak logout successful");
        } catch (WebClientResponseException ex) {
            log.error("Keycloak logout error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    @SuppressWarnings("unchecked")
    public void register(RegisterRequest request) {
        String adminToken = getAdminToken();

        Map<String, Object> userPayload = Map.of(
                "email", request.email(),
                "username", request.username(),
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
            log.info("Keycloak register successful for user: {}", request.username());
        } catch (WebClientResponseException ex) {
            log.error("Keycloak register error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    @SuppressWarnings("unchecked")
    private String getAdminToken() {
        try {
            Map<String, Object> response = keycloakWebClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Keycloak getAdminToken response: {}", response);
            return (String) response.get("access_token");
        } catch (WebClientResponseException ex) {
            log.error("Keycloak getAdminToken error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new KeycloakException(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
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
