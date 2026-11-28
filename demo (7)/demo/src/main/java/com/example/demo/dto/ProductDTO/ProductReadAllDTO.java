package com.example.demo.dto.ProductDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductReadAllDTO {
    private Integer page;
    private Integer size;
    private Integer stock;
    private String imageUrl;
    private String videoUrl;
    private String name;
    private BigDecimal price;
    private String description;
    private Long categoryId;
}
