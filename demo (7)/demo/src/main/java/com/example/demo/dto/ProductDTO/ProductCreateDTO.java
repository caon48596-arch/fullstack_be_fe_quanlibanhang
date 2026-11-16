package com.example.demo.dto.ProductDTO;

import lombok.Data;

@Data
public class ProductCreateDTO {
    private String name;
    private Double price;
    private Integer stock;
    private Long categoryId;
    private String imageUrl;
    private String videoUrl;
}
