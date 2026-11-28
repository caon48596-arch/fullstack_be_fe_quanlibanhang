package com.example.demo.service.search;

import com.example.demo.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Strategy tìm kiếm và lọc sản phẩm (tuân thủ SRP & OCP).
 * Cho phép mở rộng thêm Elasticsearch / Meilisearch / PostgreSQL Full-Text Search
 * mà không phải chỉnh sửa ProductService.
 */
public interface ProductSearchStrategy {

    boolean supports(String normalizedKeyword);

    Page<Product> search(
            Long categoryId,
            String normalizedKeyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);
}
