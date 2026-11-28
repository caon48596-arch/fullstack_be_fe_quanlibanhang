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
@Data
@Table(name = "OrderItems", indexes = {
    @Index(name = "idx_order_items_order_id", columnList = "Order_id"),
    @Index(name = "idx_order_items_product_id", columnList = "Product_id")
})
@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantity;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "Order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "Product_id")
    private Product product;

    private String status = OrderStatus.PENDING.name();
}
