package com.example.demo.service;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.config.CacheConfig;
import com.example.demo.entity.Category;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.contract.CategoryOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService implements CategoryOperations {

    private final CategoryRepository categoryRepository;

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public Category createCategory(String name) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new ConflictException("Lỗi :" + name + "đã tồn tại");
        }
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    /** @deprecated Sử dụng {@link #createCategory(String)} */
    @Deprecated
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public Category create(String name) {
        return createCategory(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("lỗi k tìm thấy " + name));
    }

    /** @deprecated Sử dụng {@link #getCategoryByName(String)} */
    @Deprecated
    @Transactional(readOnly = true)
    public Category get(String name) {
        return getCategoryByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CATEGORIES, key = "'all'")
    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("hiện tại k có cái nào cả");
        }
        return categories;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesOrEmpty() {
        try {
            return getAllCategories();
        } catch (ResourceNotFoundException ex) {
            return Collections.emptyList();
        }
    }

    /** @deprecated Sử dụng {@link #getAllCategories()} */
    @Deprecated
    @Transactional(readOnly = true)
    public List<Category> getAll() {
        return getAllCategories();
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("không tìm thấy " + id + " để xóa");
        }
        categoryRepository.deleteById(id);
    }

    /** @deprecated Sử dụng {@link #deleteCategory(Long)} */
    @Deprecated
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public void delete(Long id) {
        deleteCategory(id);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public Category updateCategory(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("không tìm thấy danh mục"));

        category.setName(name);
        return categoryRepository.save(category);
    }

    /** @deprecated Sử dụng {@link #updateCategory(Long, String)} */
    @Deprecated
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS_BY_CATEGORY, allEntries = true)
    })
    public Category update(Long id, String name) {
        return updateCategory(id, name);
    }
}
