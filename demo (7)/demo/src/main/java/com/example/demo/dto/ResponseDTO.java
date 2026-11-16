package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO<T> {
    private String code;
    private String message;
    private T data;

    public static <T> ResponseDTO<T> success(T data) {
        return new ResponseDTO<>("200", "thành công", data);
    }

    public static <T> ResponseDTO<T> error(String message, String code) {
        return new ResponseDTO<>(code, message, null);
    }
}
