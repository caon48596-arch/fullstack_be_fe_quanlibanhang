package com.example.demo.service;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.config.CacheConfig;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.contract.ProductOperations;
import com.example.demo.service.search.JpqlProductSearchStrategy;
import com.example.demo.service.search.ProductSearchStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ProductService implements ProductOperations {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final List<ProductSearchStrategy> searchStrategies;

    @Autowired
    public ProductService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            @Autowired(required = false) List<ProductSearchStrategy> searchStrategies) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.searchStrategies = (searchStrategies != null && !searchStrategies.isEmpty())
                ? List.copyOf(searchStrategies)
                : List.of(new JpqlProductSearchStrategy(productRepository));
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public Product createProduct(String name, BigDecimal price, int stock, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("không tìm thấy danh mục"));

        Product product = new Product();
        product.setName(name);
        product.setStock(stock);
        product.setPrice(normalizeMoney(price));
        product.setCategory(category);
        product.setDeleted(false);
        return productRepository.save(product);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public Product createProduct(Product product) {
        product.setDeleted(false);
        if (product.getPrice() != null) {
            product.setPrice(normalizeMoney(product.getPrice()));
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            categoryRepository.findById(product.getCategory().getId())
                    .ifPresent(product::setCategory);
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productRepository.findByDeletedFalseOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PRODUCT_DETAIL, key = "#id", unless = "#result == null")
    public Product getProductById(Long id) {
        return productRepository.findByIdAndDeletedFalse(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, key = "#categoryId")
    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndDeletedFalseOrderByIdDesc(categoryId);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT_DETAIL, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public Product updateProduct(Long id, Product productDetails) {
        return productRepository.findByIdAndDeletedFalse(id).map(product -> {
            product.setName(productDetails.getName());
            product.setPrice(normalizeMoney(productDetails.getPrice()));
            product.setDescription(productDetails.getDescription());
            if (productDetails.getStock() != null) {
                product.setStock(productDetails.getStock());
            }
            if (productDetails.getCategory() != null && productDetails.getCategory().getId() != null) {
                categoryRepository.findById(productDetails.getCategory().getId())
                        .ifPresent(product::setCategory);
            }
            if (productDetails.getImageUrl() != null) {
                product.setImageUrl(productDetails.getImageUrl());
            }
            if (productDetails.getVideoUrl() != null) {
                product.setVideoUrl(productDetails.getVideoUrl());
            }
            return productRepository.save(product);
        }).orElse(null);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT_DETAIL, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    })
    public boolean softDeleteProduct(Long id) {
        return productRepository.findByIdAndDeletedFalse(id).map(product -> {
            product.setDeleted(true);
            productRepository.save(product);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return productRepository.findByDeletedFalse(PageRequest.of(safePage, safeSize, Sort.by("id").descending()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getFilteredProducts(
            int page,
            int size,
            Long categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Sort sort = switch (sortBy != null ? sortBy : "newest") {
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "name-asc" -> Sort.by("name").ascending();
            default -> Sort.by("id").descending();
        };

        String normalizedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        for (ProductSearchStrategy strategy : searchStrategies) {
            if (strategy.supports(normalizedKeyword)) {
                try {
                    return strategy.search(categoryId, normalizedKeyword, minPrice, maxPrice, pageable);
                } catch (Exception ex) {
                    log.debug("[ProductSearchStrategy Fallback] Chiến lược {} gặp lỗi cho keyword '{}': {}",
                            strategy.getClass().getSimpleName(), normalizedKeyword, ex.getMessage());
                }
            }
        }

        return productRepository.findFilteredProducts(categoryId, normalizedKeyword, minPrice, maxPrice, pageable);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT_DETAIL, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void reduceStock(long id, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Số lượng phải lớn hơn 0");
        }
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("không có sẵn phẩm"));

        if (product.isDeleted()) {
            throw new BusinessException("Sản phẩm đã ngừng kinh doanh");
        }

        int currentStock = product.getStock() != null ? product.getStock() : 0;
        if (currentStock < quantity) {
            throw new BusinessException("sản phẩm k đủ");
        }

        product.setStock(currentStock - quantity);
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String name, BigDecimal minprice, int size, int page) {
        return productRepository.searchProduct(
                name,
                minprice != null ? minprice : BigDecimal.ZERO,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)));
    }
}
