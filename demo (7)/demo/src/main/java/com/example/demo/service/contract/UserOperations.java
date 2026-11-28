package com.example.demo.service.contract;

import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.dto.UserDTO.UserUpdateDTO;
import com.example.demo.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hợp đồng quản lý thông tin Người dùng (ISP & DIP).
 */
public interface UserOperations {

    User registerUser(UserRegisterDTO user);

    UserLoginDTO login(String identifier, String password);

    User logout(Long id);

    void markOfflineByEmail(String email);

    List<User> getAllUsers();

    User updateUser(String email, UserUpdateDTO updateDTO);

    Map<String, String> getUserProfile(String email);

    boolean existsByIdentifier(String identifier);

    Optional<User> findByIdentifier(String identifier);
}
