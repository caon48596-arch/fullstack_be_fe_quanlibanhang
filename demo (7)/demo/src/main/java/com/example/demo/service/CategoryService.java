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

    // Create
    public Category create(String name) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new RuntimeException("Lỗi :" + name + "đã tồn tại");
        }
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    // Read
    public Category get(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("lỗi k tìm thấy " + name));
    }

    // ReadFull
    public List<Category> getAll() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            throw new RuntimeException("hiện tại k có cái nào cả");
        }
        return categories;
    }

    // Delete
    public void delete(Long id) {
        if (categoryRepository.existsById(id)) {
            throw new RuntimeException("không tìm thấy " + id + " để xóa");
        }
        categoryRepository.deleteById(id);
    }

    // Update
    public Category update(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("không tìm thấy sản phâm"));

        category.setName(name);
        return categoryRepository.save(category);
    }
}
