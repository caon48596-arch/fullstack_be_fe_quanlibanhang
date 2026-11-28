package com.example.demo.service.contract;

import com.example.demo.entity.Category;

import java.util.List;

/**
 * Hợp đồng nghiệp vụ quản lý Danh mục sản phẩm (DIP).
 */
public interface CategoryOperations {

    Category createCategory(String name);

    Category getCategoryByName(String name);

    List<Category> getAllCategories();

    List<Category> getAllCategoriesOrEmpty();

    void deleteCategory(Long id);

    Category updateCategory(Long id, String name);
}
