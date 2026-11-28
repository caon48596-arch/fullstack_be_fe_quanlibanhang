package com.example.demo.dto.OrderDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentWebhookDTO {
    @NotNull(message = "Thiếu orderId")
    private Long orderId;

    @NotBlank(message = "Thiếu transactionId")
    private String transactionId;

    @NotBlank(message = "Thiếu trạng thái thanh toán (SUCCESS hoặc FAILED)")
    private String status;

    private BigDecimal amount;
    private String provider;
}
