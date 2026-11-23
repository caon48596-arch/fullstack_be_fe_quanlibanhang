package com.example.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Category;

import com.example.demo.dto.CategoryDTO.CategoryCreateDTO;
import com.example.demo.dto.CategoryDTO.CategoryDeleteDTO;
import com.example.demo.dto.CategoryDTO.CategoryReadOneDTO;
import com.example.demo.dto.CategoryDTO.CategoryUpdateDTO;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.service.CategoryService;

@RequestMapping("/api/categories")
@RestController
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping("/Create")
    public ResponseEntity<ResponseDTO<String>> createCategory(@RequestBody CategoryCreateDTO categoryCreateDTO) {
        try {
            categoryService.createCategory(categoryCreateDTO.getName());
            return ResponseEntity.ok(ResponseDTO.success("khởi tạo thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ResponseDTO.error("tên danh mục đã tồn tại", "401"));
        }
    }

    @GetMapping("/Read")
    public ResponseEntity<ResponseDTO<String>> getCategory(@RequestBody CategoryReadOneDTO categoryReadOneDTO) {
        try {
            categoryService.getCategoryByName(categoryReadOneDTO.getName());
            return ResponseEntity.ok(ResponseDTO.success("đã tìm thấy "));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ResponseDTO.error("không tìm  thấy tên danh mục", "401"));
        }
    }

    @GetMapping("/ReadAll")
    public ResponseEntity<ResponseDTO<String>> getAllCategoriesOverview() {
        try {
            return ResponseEntity.ok(ResponseDTO.success("đã tìm thấy "));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ResponseDTO.error("không tìm thấy tên danh mục", "401"));
        }
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        try {
            return ResponseEntity.ok(categoryService.getAllCategories());
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @PostMapping("/Update")
    public ResponseEntity<ResponseDTO<String>> updateCategory(@RequestBody CategoryUpdateDTO categoryUpdateDTO) {
        try {
            categoryService.updateCategory(categoryUpdateDTO.getId(), categoryUpdateDTO.getName());
            return ResponseEntity.ok(ResponseDTO.success("đã cặp nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ResponseDTO.error("không tìm thấy tên và id để cặp nhật", "401"));
        }
    }

    @PostMapping("/Delete")
    public ResponseEntity<ResponseDTO<String>> deleteCategory(@RequestBody CategoryDeleteDTO categoryDeleteDTO) {
        try {
            categoryService.deleteCategory(categoryDeleteDTO.getId());
            return ResponseEntity.ok(ResponseDTO.success("đã xóa thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ResponseDTO.error("không tìm thấy tên và id để xóa", "401"));
        }
    }
}
