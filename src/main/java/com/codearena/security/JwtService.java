package com.codearena.security;

import com.codearena.entity.User;

import io.jsonwebtoken.Claims;

public interface JwtService {

    String generateToken(User user);

    Claims validateAndExtractClaims(String token);

    boolean isTokenValid(String token);
}
