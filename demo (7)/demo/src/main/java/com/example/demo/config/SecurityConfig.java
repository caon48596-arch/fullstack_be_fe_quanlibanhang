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
                        // 1. Cho phép tất cả các request OPTIONS (CORS Preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. Cho phép xem danh sách sản phẩm, danh mục và kiểm tra mã giảm giá tự do (GET)
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**", "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/validate/**").permitAll()

                        // 3. Cho phép API đăng nhập, đăng ký và health check không cần token
                        .requestMatchers("/api/v1/system/health", "/api/users/register", "/api/users/login", "/api/users/request-otp", "/api/users/login-otp", "/error").permitAll()

                        // 4. Các quyền quản trị viên (Chỉ Admin mới được truy cập)
                        // Quản lý Dashboard doanh thu, thống kê
                        .requestMatchers("/api/dashboard/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // Thêm, sửa, xóa sản phẩm
                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // Thêm, sửa, xóa danh mục
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // Quản lý mã giảm giá (Xem toàn bộ coupon, tạo mã, xóa mã)
                        .requestMatchers(HttpMethod.GET, "/api/coupons").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/coupons", "/api/coupons/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/coupons/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // Quản trị đơn hàng (Cập nhật trạng thái đơn, xem toàn bộ chi tiết đơn)
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**/status").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/ReadAll").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 5. Khách hàng: Tạo hóa đơn / Thanh toán và xem đơn hàng yêu cầu đăng nhập
                        .requestMatchers(HttpMethod.POST, "/api/orders/CreateAndSum").authenticated()
                        .requestMatchers("/api/orders/my-orders", "/api/orders/my-order-items").authenticated()

                        // 6. Tất cả request còn lại (thông tin profile,...) yêu cầu đăng nhập
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
