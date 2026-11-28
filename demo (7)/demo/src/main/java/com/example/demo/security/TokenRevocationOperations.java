package com.example.demo.security;

import java.util.Date;

/**
 * Abstraction thu hồi Access Token (Blacklist) và xoay vòng Refresh Token (Rotation) (tuân thủ DIP).
 */
public interface TokenRevocationOperations {

    void revokeAccessToken(String jti, Date expiration);

    boolean isTokenRevoked(String jti);

    void registerRefreshToken(String jti, String subject, long ttlMillis);

    boolean consumeRefreshToken(String jti, String expectedSubject);

    void revokeRefreshToken(String jti);
}
