package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.entity.Category;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    public final CategoryRepository categoryRepository;

    public Category createCategory(String name) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Lỗi :" + name + "đã tồn tại");
        }
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    /** @deprecated Sử dụng {@link #createCategory(String)} */
    @Deprecated
    public Category create(String name) {
        return createCategory(name);
    }

    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("lỗi k tìm thấy " + name));
    }

    /** @deprecated Sử dụng {@link #getCategoryByName(String)} */
    @Deprecated
    public Category get(String name) {
        return getCategoryByName(name);
    }

    public List<Category> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            throw new RuntimeException("hiện tại k có cái nào cả");
        }
        return categories;
    }

    /** @deprecated Sử dụng {@link #getAllCategories()} */
    @Deprecated
    public List<Category> getAll() {
        return getAllCategories();
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("không tìm thấy " + id + " để xóa");
        }
        categoryRepository.deleteById(id);
    }

    /** @deprecated Sử dụng {@link #deleteCategory(Long)} */
    @Deprecated
    public void delete(Long id) {
        deleteCategory(id);
    }

    public Category updateCategory(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("không tìm thấy sản phâm"));

        category.setName(name);
        return categoryRepository.save(category);
    }

    /** @deprecated Sử dụng {@link #updateCategory(Long, String)} */
    @Deprecated
    public Category update(Long id, String name) {
        return updateCategory(id, name);
    }
}
