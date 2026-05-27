package com.skyviet.auth.config;

import com.skyviet.auth.service.KeycloakException;
import com.skyviet.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<ApiResponse<Void>> handleKeycloak(KeycloakException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getResponseBody()));
    }
}
