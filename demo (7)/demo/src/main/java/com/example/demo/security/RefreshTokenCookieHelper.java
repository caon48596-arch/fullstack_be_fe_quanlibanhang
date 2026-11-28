package com.example.demo.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Tách riêng trách nhiệm đọc/ghi HttpOnly Cookie cho Refresh Token khỏi Controller & AuthService (SRP).
 */
@Component
public class RefreshTokenCookieHelper {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final boolean cookieSecure;

    public RefreshTokenCookieHelper(@Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public void writeRefreshCookie(HttpServletResponse response, String refreshToken, long maxAgeMillis) {
        if (response == null || !StringUtils.hasText(refreshToken)) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ofMillis(Math.max(0L, maxAgeMillis)))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String extractRefreshCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
