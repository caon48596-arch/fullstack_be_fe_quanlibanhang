package com.example.demo.security;

import io.jsonwebtoken.Claims;

/**
 * Abstraction quản lý phát hành và giải mã JWT Token (tuân thủ DIP).
 */
public interface TokenProvider {

    String generateToken(String username, String role);

    String generateAccessToken(String username, String role);

    String generateRefreshToken(String username, String role);

    long getRefreshExpirationMs();

    Claims parseClaims(String token);

    String extractUsername(String token);

    String extractRole(String token);

    String extractJti(Claims claims);

    String extractTokenType(Claims claims);

    boolean validateJwtToken(String authToken);
}
