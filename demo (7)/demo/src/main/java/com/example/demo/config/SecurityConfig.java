package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.example.demo.security.JwtAuthFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight & public endpoints
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**", "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/validate/**").permitAll()
                        .requestMatchers("/api/v1/system/health", "/api/login", "/api/users/register", "/api/users/login", "/api/users/request-otp", "/api/users/login-otp", "/error").permitAll()

                        // Admin only
                        .requestMatchers("/api/dashboard/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/coupons").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/coupons", "/api/coupons/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/coupons/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/status", "/api/orders/item/*/status").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/ReadAll").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // Authenticated users
                        .requestMatchers(HttpMethod.POST, "/api/orders/CreateAndSum").authenticated()
                        .requestMatchers("/api/orders/my-orders", "/api/orders/my-order-items").authenticated()
                        .anyRequest().authenticated());

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
