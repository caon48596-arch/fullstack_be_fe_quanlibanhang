package com.example.demo.service.search;

import com.example.demo.Repository.ProductRepository;
import com.example.demo.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Chiến lược tìm kiếm ưu tiên dùng MySQL 8 FULLTEXT INDEX khi chạy trên MySQL và từ khóa đủ độ dài.
 * Đóng gói hoàn toàn việc nhận diện JDBC Dialect & cú pháp Boolean Mode ra khỏi ProductService (SRP).
 */
@Component
@Order(10)
public class MySqlFullTextProductSearchStrategy implements ProductSearchStrategy {

    private final ProductRepository productRepository;
    private final DataSource dataSource;
    private volatile Boolean mysqlDatabase;

    public MySqlFullTextProductSearchStrategy(
            ProductRepository productRepository,
            @Autowired(required = false) DataSource dataSource) {
        this.productRepository = productRepository;
        this.dataSource = dataSource;
    }

    @Override
    public boolean supports(String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.length() < 3 || !isMySqlDatabase()) {
            return false;
        }
        return !buildMySqlBooleanQuery(normalizedKeyword).isEmpty();
    }

    @Override
    public Page<Product> search(
            Long categoryId,
            String normalizedKeyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        String booleanQuery = buildMySqlBooleanQuery(normalizedKeyword);
        return productRepository.findFilteredProductsFullText(
                categoryId,
                booleanQuery,
                minPrice,
                maxPrice,
                pageable);
    }

    private boolean isMySqlDatabase() {
        if (mysqlDatabase != null) {
            return mysqlDatabase;
        }
        if (dataSource == null) {
            mysqlDatabase = false;
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName();
            mysqlDatabase = productName != null && productName.toLowerCase().contains("mysql");
        } catch (Exception ex) {
            mysqlDatabase = false;
        }
        return mysqlDatabase;
    }

    private String buildMySqlBooleanQuery(String rawKeyword) {
        String sanitized = rawKeyword.replaceAll("[+\\-><()~*\"@]+", " ").trim();
        if (sanitized.isEmpty()) {
            return "";
        }
        return Arrays.stream(sanitized.split("\\s+"))
                .filter(token -> token.length() >= 2)
                .map(token -> "+" + token + "*")
                .collect(Collectors.joining(" "));
    }
}
