package com.example.demo.service.contract;

import com.example.demo.entity.Product;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hợp đồng nghiệp vụ quản lý Sản phẩm và Tồn kho (DIP).
 */
public interface ProductOperations {

    Product createProduct(String name, BigDecimal price, int stock, Long categoryId);

    Product createProduct(Product product);

    List<Product> getAllActiveProducts();

    Product getProductById(Long id);

    List<Product> getProductsByCategory(Long categoryId);

    Product updateProduct(Long id, Product productDetails);

    boolean softDeleteProduct(Long id);

    Page<Product> getAllProducts(int page, int size);

    Page<Product> getFilteredProducts(
            int page,
            int size,
            Long categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy);

    void reduceStock(long id, int quantity);

    Page<Product> searchProducts(String name, BigDecimal minprice, int size, int page);
}
