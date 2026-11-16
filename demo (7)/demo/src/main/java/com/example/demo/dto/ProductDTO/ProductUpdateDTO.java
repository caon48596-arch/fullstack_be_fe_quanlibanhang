package com.example.demo.dto.ProductDTO;

import lombok.Data;

@Data
public class ProductUpdateDTO {
    private Long id;
    private Integer quantity;
    private String imageUrl;
    private String videoUrl;
    private String name;
    private Double price;
    private String description;
    private Long categoryId;
}
