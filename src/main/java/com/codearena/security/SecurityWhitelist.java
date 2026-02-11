package com.codearena.security;

public final class SecurityWhitelist {

    private SecurityWhitelist() {}

    public static final String[] PUBLIC_URLS = {
            "/api/auth/**",
            "/api/leaderboards/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/h2-console/**",
            "/favicon.ico",
            "/error"
    };
}
