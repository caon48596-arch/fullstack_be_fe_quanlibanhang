package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil implements TokenProvider {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:900000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        return generateAccessToken(username, role);
    }

    public String generateAccessToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .claim("role", role)
                .claim("type", TOKEN_TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .claim("role", role)
                .claim("type", TOKEN_TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + jwtRefreshExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public long getRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public String extractJti(Claims claims) {
        return claims != null ? claims.getId() : null;
    }

    public String extractTokenType(Claims claims) {
        if (claims == null) return TOKEN_TYPE_ACCESS;
        Object type = claims.get("type");
        return type != null ? type.toString() : TOKEN_TYPE_ACCESS;
    }

    public boolean validateJwtToken(String authToken) {
        try {
            parseClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
