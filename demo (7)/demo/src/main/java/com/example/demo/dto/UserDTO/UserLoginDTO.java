package com.example.demo.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginDTO {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String token;
}
