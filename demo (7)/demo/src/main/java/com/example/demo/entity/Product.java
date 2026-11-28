package com.example.demo.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Products", indexes = {
    @Index(name = "idx_products_category_price", columnList = "Category_id, price"),
    @Index(name = "idx_products_deleted_category_price", columnList = "deleted, Category_id, price"),
    @Index(name = "idx_products_deleted_price", columnList = "deleted, price"),
    @Index(name = "idx_products_name", columnList = "name")
})
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    private Integer stock;
    private String description;
    private String imageUrl;
    private String videoUrl;

    @ManyToOne
    @JoinColumn(name = "Category_id")
    private Category category;

    @Column(nullable = false)
    private boolean deleted = false;

    public Product(Long id, String name, BigDecimal price, Integer stock, String description,
                   String imageUrl, String videoUrl, Category category) {
        this(id, name, price, stock, description, imageUrl, videoUrl, category, false);
    }
}
