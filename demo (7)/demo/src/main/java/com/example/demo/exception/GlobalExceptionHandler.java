package com.example.demo.exception;

import com.example.demo.dto.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Xử lý lỗi tập trung cho toàn bộ REST API, trả HTTP Status chuẩn (400/401/403/404/409/500)
 * và ẩn stack trace chi tiết ở môi trường production.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseDTO<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseDTO.error(ex.getMessage(), "404"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ResponseDTO<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseDTO.error(ex.getMessage(), "409"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseDTO<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseDTO.error(ex.getMessage(), "401"));
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ResponseDTO<Void>> handleForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ResponseDTO.error(
                        ex.getMessage() != null ? ex.getMessage() : "Bạn không có quyền truy cập tài nguyên này.",
                        "403"));
    }

    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public ResponseEntity<ResponseDTO<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseDTO.error(ex.getMessage(), "400"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseDTO.error(details.isBlank() ? "Dữ liệu đầu vào không hợp lệ." : details, "400"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseDTO<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("[DataIntegrityViolation] {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseDTO.error("Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc cơ sở dữ liệu.", "409"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDTO<Void>> handleRuntime(RuntimeException ex) {
        log.warn("[RuntimeException] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseDTO.error(ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ.", "400"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Void>> handleUnexpected(Exception ex) {
        log.error("[Unexpected Error]", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDTO.error("Lỗi hệ thống nội bộ. Vui lòng thử lại sau.", "500"));
    }
}
