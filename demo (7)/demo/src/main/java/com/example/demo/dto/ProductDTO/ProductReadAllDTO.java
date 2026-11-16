package com.example.demo.dto.ProductDTO;

import lombok.Data;

@Data
public class ProductReadAllDTO {
    private Integer page;
    private Integer size;
    private Integer stock;
    private String imageUrl;
    private String videoUrl;
    private String name;
    private Double price;
    private String description;
    private Long categoryId;
}
