package com.example.demo.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationOperations tokenRevocationOperations;

    @Autowired
    public JwtAuthFilter(
            TokenProvider tokenProvider,
            UserDetailsService userDetailsService,
            @Autowired(required = false) TokenRevocationOperations tokenRevocationOperations) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.tokenRevocationOperations = tokenRevocationOperations;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null) {
                Claims claims = tokenProvider.parseClaims(jwt);
                String tokenType = tokenProvider.extractTokenType(claims);
                String jti = tokenProvider.extractJti(claims);

                if (JwtUtil.TOKEN_TYPE_REFRESH.equals(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if (tokenRevocationOperations != null && tokenRevocationOperations.isTokenRevoked(jti)) {
                    logger.debug("Token JWT đã bị thu hồi (blacklist): " + jti);
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = claims.getSubject();
                Object roleClaim = claims.get("role");

                if (StringUtils.hasText(username)) {
                    UserDetails userDetails;
                    if (roleClaim != null && StringUtils.hasText(roleClaim.toString())) {
                        String role = roleClaim.toString().trim();
                        userDetails = new User(username, "", List.of(new SimpleGrantedAuthority(role)));
                    } else {
                        userDetails = userDetailsService.loadUserByUsername(username);
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.debug("Token JWT không hợp lệ hoặc đã hết hạn: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
