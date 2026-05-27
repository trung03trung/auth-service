package com.skyviet.auth.dto;

import java.util.List;

public record UserInfo(
    String id,
    String email,
    String firstName,
    String lastName,
    List<String> roles
) {}
