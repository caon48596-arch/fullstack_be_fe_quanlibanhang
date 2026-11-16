package com.example.demo.dto.UserDTO;

import lombok.Data;

@Data
public class OtpRequestDTO {
    private String email;
    private String type; // "REGISTER" or "LOGIN"
}
