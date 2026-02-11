package com.codearena.dto;

public record AuthResponse(String token, String tokenType, UserDto user) {
    public static final String BEARER_TOKEN_TYPE = "Bearer";

    public static AuthResponse bearer(String token, UserDto user) {
        return new AuthResponse(token, BEARER_TOKEN_TYPE, user);
    }
}
