package com.example.demo.dto.UserDTO;

import lombok.Data;

@Data
public class OtpLoginDTO {
    private String email;
    private String password;
    private String otp;
}
