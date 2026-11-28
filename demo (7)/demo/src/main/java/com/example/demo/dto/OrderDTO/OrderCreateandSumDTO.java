package com.example.demo.dto.OrderDTO;
import java.util.List;

import com.example.demo.entity.OrderItem;

import lombok.Data;

@Data
public class OrderCreateandSumDTO {

    private Long id;
    private List<OrderItem> items;
    private String couponCode;
    private String email;
    private String idempotencyKey;
}
