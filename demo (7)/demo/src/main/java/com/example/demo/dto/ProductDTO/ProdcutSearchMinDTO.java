package com.example.demo.dto.ProductDTO;

import lombok.Data;

@Data
public class ProdcutSearchMinDTO {
    private String name;
    private Double minprice;
    private Integer size;
    private Integer page;
}
