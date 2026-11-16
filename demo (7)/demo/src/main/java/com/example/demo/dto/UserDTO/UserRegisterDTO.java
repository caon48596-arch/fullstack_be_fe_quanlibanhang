package com.example.demo.dto.UserDTO;

import lombok.Data;

@Data
public class UserRegisterDTO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String otp;
}
