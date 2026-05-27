package com.skyviet.auth.service;

import lombok.Getter;

@Getter
public class KeycloakException extends RuntimeException {
    private final int status;
    private final String responseBody;

    public KeycloakException(int status, String responseBody) {
        super("Keycloak error: " + status);
        this.status = status;
        this.responseBody = responseBody;
    }
}
