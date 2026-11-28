package com.example.demo.dto.ProductDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductUpdateDTO {
    private Long id;
    private Integer quantity;
    private String imageUrl;
    private String videoUrl;
    private String name;
    private BigDecimal price;
    private String description;
    private Long categoryId;
}
