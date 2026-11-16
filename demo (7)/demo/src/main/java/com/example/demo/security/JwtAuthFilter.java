package com.example.demo.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.lang.Override;
import org.springframework.util.StringUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.userdetails.UserDetailsService;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;
    
        private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        // Chuẩn OAuth2: Token phải bắt đầu bằng chữ "Bearer "
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7); // Cắt bỏ 7 ký tự "Bearer " để lấy lõi Token
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Bước 1: Lấy chuỗi JWT từ Header của Request
            String jwt = parseJwt(request);

            // Bước 2: Nếu có Token và Token đó hợp lệ (không hết hạn, không bị sửa)
            if (jwt != null && jwtUtil.validateJwtToken(jwt)) {

                // Trích xuất Username từ Token
                String username = jwtUtil.extractUsername(jwt);

                // Bước 3: Móc thông tin chi tiết của User từ Database lên (thông qua username)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Bước 4: Tạo giấy thông hành (Authentication) và nhét vào SecurityContext
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Báo cho Spring Security biết: "Người này đã hợp lệ, cho phép đi qua"
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Không thể thiết lập xác thực người dùng: {}", e);
        }

        // Dù có vé hay không có vé (như khách vãng lai), vẫn cho request đi tiếp để
        // Lưới lọc SecurityFilterChain ở Config tự quyết định chặn hay thả
        filterChain.doFilter(request, response);
    }


}
