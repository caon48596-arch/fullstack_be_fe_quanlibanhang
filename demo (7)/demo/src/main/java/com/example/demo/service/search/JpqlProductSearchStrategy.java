package com.example.demo.service.search;

import com.example.demo.Repository.ProductRepository;
import com.example.demo.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Chiến lược tìm kiếm JPQL tiêu chuẩn tương thích mọi RDBMS (MySQL, H2, PostgreSQL).
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class JpqlProductSearchStrategy implements ProductSearchStrategy {

    private final ProductRepository productRepository;

    @Override
    public boolean supports(String normalizedKeyword) {
        return true;
    }

    @Override
    public Page<Product> search(
            Long categoryId,
            String normalizedKeyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return productRepository.findFilteredProducts(
                categoryId,
                normalizedKeyword,
                minPrice,
                maxPrice,
                pageable);
    }
}
