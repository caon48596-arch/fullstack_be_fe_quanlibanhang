package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public Product createProduct(String name, double price, int stock, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("không tìm thấy danh mục"));

        Product product = new Product();
        product.setName(name);
        product.setStock(stock);
        product.setPrice(price);
        product.setCategory(category);
        return productRepository.save(product);
    }

    public Page<Product> getAllProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @Transactional
    public void reduceStock(long id, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("không có sẵn phẩm"));

        int currentStock = product.getStock() != null ? product.getStock() : 0;
        if (currentStock < quantity) {
            throw new RuntimeException("sản phẩm k đủ");
        }

        product.setStock(currentStock - quantity);
        productRepository.save(product);
    }

    public Page<Product> searchProducts(String name, double minprice, int size, int page) {
        return productRepository.searchProduct(name, minprice, PageRequest.of(page, size));
    }
}
