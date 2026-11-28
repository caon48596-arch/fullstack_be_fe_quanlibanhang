package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gắn Correlation ID (`X-Request-Id`) vào SLF4J MDC cho mọi request HTTP để truy vết log đầu-cuối
 * và ghi nhận độ trễ thực thi API (Observability).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE - 1)
@Slf4j
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            requestId = UUID.randomUUID().toString();
        }

        long startNs = System.nanoTime();
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            String uri = request.getRequestURI();
            if (!uri.startsWith("/actuator") && !uri.equals("/api/v1/system/health")) {
                log.info("[HTTP] {} {} -> status={} duration={}ms",
                        request.getMethod(), uri, response.getStatus(), durationMs);
            }
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}
