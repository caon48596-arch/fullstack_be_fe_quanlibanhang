package com.example.demo.dto.ProductDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProdcutSearchMinDTO {
    private String name;
    private BigDecimal minprice;
    private Integer size;
    private Integer page;
}
