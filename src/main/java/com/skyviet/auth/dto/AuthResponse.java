package com.skyviet.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType
) {}
