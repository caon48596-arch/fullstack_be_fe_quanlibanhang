package com.example.demo.service;

import com.example.demo.Repository.UserRepository;
import com.example.demo.config.CacheConfig;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.dto.UserDTO.UserUpdateDTO;
import com.example.demo.entity.User;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.security.TokenProvider;
import com.example.demo.service.contract.UserOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserOperations {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider jwtUtil;

    @Override
    @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    public User registerUser(UserRegisterDTO user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {
            throw new ConflictException("Lỗi :Email '" + existingUser.get().getEmail() + "' đã tồn tại");
        }

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getEmail() != null && user.getEmail().contains("@")) {
            newUser.setEmail(user.getEmail());
        } else if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            newUser.setPhone(user.getPhone());
        } else {
            if (user.getEmail() != null && !user.getEmail().contains("@")) {
                newUser.setPhone(user.getEmail());
                newUser.setEmail(null);
            } else {
                newUser.setEmail(user.getEmail());
            }
        }

        newUser.setRole("ROLE_CLIENT");
        return userRepository.save(newUser);
    }

    @Override
    public UserLoginDTO login(String identifier, String password) {
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new UnauthorizedException("Đăng nhập thất bại: Tài khoản không tồn tại!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Đăng nhập thất bại: Sai mật khẩu rồi bạn ơi!");
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
                null,
                token);
    }

    @Override
    public User logout(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("không tìm thấy người dùng"));

        user.setOnline(false);
        userRepository.save(user);
        return user;
    }

    @Override
    public void markOfflineByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setOnline(false);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /** @deprecated Sử dụng {@link #getAllUsers()} */
    @Deprecated
    @Transactional(readOnly = true)
    public List<User> getAllUser() {
        return getAllUsers();
    }

    @Override
    public User updateUser(String email, UserUpdateDTO updateDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (updateDTO.getDob() != null) {
            user.setDob(updateDTO.getDob());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().isEmpty() && !updateDTO.getEmail().equals(user.getEmail())) {
            Optional<User> existingUser = userRepository.findByEmail(updateDTO.getEmail());
            if (existingUser.isPresent()) {
                throw new ConflictException("Email đã tồn tại");
            }
            user.setEmail(updateDTO.getEmail());
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Map<String, String> profile = new HashMap<>();
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("dob", user.getDob());
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());
        return profile;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        return userRepository.findByEmail(identifier).isPresent()
                || userRepository.findByPhone(identifier).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier));
    }
}
