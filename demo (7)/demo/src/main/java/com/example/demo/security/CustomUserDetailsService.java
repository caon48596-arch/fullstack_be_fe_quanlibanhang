package com.example.demo.security;

import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import com.example.demo.Repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.trim().isEmpty()) {
            throw new UsernameNotFoundException("Email người dùng không hợp lệ hoặc rỗng");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        // Lấy role từ database (ví dụ: "ROLE_ADMIN" hoặc "ROLE_CLIENT"),
        // nếu lỡ DB trống thì mặc định gán ROLE_CLIENT để an toàn hệ thống
        String role = user.getRole() != null ? user.getRole() : "ROLE_CLIENT";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(role)
                .build();
    }
}
