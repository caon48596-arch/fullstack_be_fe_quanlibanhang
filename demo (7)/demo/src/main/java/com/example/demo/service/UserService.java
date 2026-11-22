package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.dto.UserDTO.UserUpdateDTO;
import com.example.demo.entity.User;
import com.example.demo.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    public final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public User registerUser(UserRegisterDTO user) {

        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {
            throw new RuntimeException("Lỗi :Email '" + existingUser.get().getEmail() + "' đã tồn tại");
        }

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        // Hash
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        newUser.setPassword(hashedPassword);
        
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            newUser.setEmail(user.getEmail());
        } else if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            newUser.setPhone(user.getPhone());
        } else {
            // Default check if email field contains phone
            if (user.getEmail() != null && !user.getEmail().contains("@")) {
                newUser.setPhone(user.getEmail());
                newUser.setEmail(null);
            } else {
                newUser.setEmail(user.getEmail());
            }
        }
        
        newUser.setRole("ROLE_CLIENT"); // Mặc định tài khoản mới là khách
        return userRepository.save(newUser);
    }

    public UserLoginDTO login(String identifier, String password) {

        User user = userRepository.findByEmail(identifier).orElse(null);
        if (user == null) {
            user = userRepository.findByPhone(identifier)
                    .orElseThrow(() -> new RuntimeException("Đăng nhập thất bại: Tài khoản không tồn tại!"));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Đăng nhập thất bại: Sai mật khẩu rồi bạn ơi!");
        }
        
        String role = user.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "ROLE_CLIENT";
        }

        String tokenSubject = (user.getEmail() != null && !user.getEmail().trim().isEmpty())
                ? user.getEmail()
                : (user.getPhone() != null && !user.getPhone().trim().isEmpty() ? user.getPhone() : user.getUsername());

        String token = jwtUtil.generateToken(tokenSubject, role);

        user.setOnline(true);
        userRepository.save(user);
        return new UserLoginDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail() != null ? user.getEmail() : user.getPhone(),
                null, // password
                token);
    }

    public User logout(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("không tìm thấy người dùng"));

        user.setOnline(false);
        userRepository.save(user);
        return user;
    }

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    public User updateUser(String email, UserUpdateDTO updateDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (updateDTO.getDob() != null) {
            user.setDob(updateDTO.getDob());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        // Cho phép cập nhật email mới (nếu có logic kiểm tra trùng thì thêm vào)
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty() && !updateDTO.getEmail().equals(user.getEmail())) {
            Optional<User> existingUser = userRepository.findByEmail(updateDTO.getEmail());
            if (existingUser.isPresent()) {
                throw new RuntimeException("Email đã tồn tại");
            }
            user.setEmail(updateDTO.getEmail());
        }
        return userRepository.save(user);
    }
}
