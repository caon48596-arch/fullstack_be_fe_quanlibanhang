package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.dto.UserDTO.UserUpdateDTO;
import com.example.demo.entity.User;
import com.example.demo.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPhone("0912345678");
        sampleUser.setRole("ROLE_CLIENT");
        sampleUser.setOnline(false);
    }

    @Test
    @DisplayName("Đăng ký tài khoản bằng email thành công")
    void testRegisterUser_Success_WithEmail() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ROLE_CLIENT", result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký tài khoản thất bại khi email đã tồn tại")
    void testRegisterUser_EmailAlreadyExists_ThrowsException() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(dto);
        });

        assertTrue(exception.getMessage().contains("đã tồn tại"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng nhập bằng email thành công")
    void testLogin_Success_WithEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", "ROLE_CLIENT")).thenReturn("mock-jwt-token");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserLoginDTO result = userService.login("test@example.com", "123456");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("mock-jwt-token", result.getToken());
        assertTrue(sampleUser.isOnline());
    }

    @Test
    @DisplayName("Đăng nhập bằng SĐT thành công ngay cả khi email là null")
    void testLogin_Success_WithPhone() {
        User phoneUser = new User();
        phoneUser.setId(2L);
        phoneUser.setUsername("phoneuser");
        phoneUser.setPassword("encodedPassword");
        phoneUser.setEmail(null); // Không có email
        phoneUser.setPhone("0987654321");
        phoneUser.setRole("ROLE_CLIENT");

        when(userRepository.findByEmail("0987654321")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0987654321")).thenReturn(Optional.of(phoneUser));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("0987654321", "ROLE_CLIENT")).thenReturn("mock-jwt-phone-token");
        when(userRepository.save(any(User.class))).thenReturn(phoneUser);

        UserLoginDTO result = userService.login("0987654321", "123456");

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("phoneuser", result.getUsername());
        assertEquals("mock-jwt-phone-token", result.getToken());
        assertTrue(phoneUser.isOnline());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản không tồn tại")
    void testLogin_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("notfound@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login("notfound@example.com", "123456");
        });

        assertTrue(exception.getMessage().contains("Tài khoản không tồn tại"));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi mật khẩu sai")
    void testLogin_WrongPassword_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login("test@example.com", "wrongpass");
        });

        assertTrue(exception.getMessage().contains("Sai mật khẩu"));
    }

    @Test
    @DisplayName("Đăng xuất thành công")
    void testLogout_Success() {
        sampleUser.setOnline(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = userService.logout(1L);

        assertNotNull(result);
        assertFalse(result.isOnline());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Đăng xuất thất bại khi không tìm thấy user ID")
    void testLogout_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            userService.logout(999L);
        });
    }

    @Test
    @DisplayName("Lấy danh sách tất cả user")
    void testGetAllUser_Success() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<User> list = userService.getAllUser();

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Cập nhật thông tin user thành công")
    void testUpdateUser_Success() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setDob("2000-01-01");
        dto.setPhone("0999888777");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User updated = userService.updateUser("test@example.com", dto);

        assertNotNull(updated);
        assertEquals("2000-01-01", updated.getDob());
        assertEquals("0999888777", updated.getPhone());
    }

    @Test
    @DisplayName("Cập nhật thông tin thất bại khi email mới bị trùng")
    void testUpdateUser_EmailAlreadyExists_ThrowsException() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("existing@example.com");

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail("existing@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(otherUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser("test@example.com", dto);
        });

        assertTrue(exception.getMessage().contains("Email đã tồn tại"));
    }
}
