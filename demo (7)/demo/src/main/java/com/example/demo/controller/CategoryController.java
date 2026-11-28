package com.example.demo.controller;

import com.example.demo.dto.CategoryDTO.CategoryCreateDTO;
import com.example.demo.dto.CategoryDTO.CategoryDeleteDTO;
import com.example.demo.dto.CategoryDTO.CategoryReadOneDTO;
import com.example.demo.dto.CategoryDTO.CategoryUpdateDTO;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.entity.Category;
import com.example.demo.service.contract.CategoryOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryOperations categoryService;

    @PostMapping("/Create")
    public ResponseEntity<ResponseDTO<String>> createCategory(@RequestBody CategoryCreateDTO categoryCreateDTO) {
        categoryService.createCategory(categoryCreateDTO.getName());
        return ResponseEntity.ok(ResponseDTO.success("khởi tạo thành công"));
    }

    @GetMapping("/Read")
    public ResponseEntity<ResponseDTO<String>> getCategory(@RequestBody CategoryReadOneDTO categoryReadOneDTO) {
        categoryService.getCategoryByName(categoryReadOneDTO.getName());
        return ResponseEntity.ok(ResponseDTO.success("đã tìm thấy "));
    }

    @GetMapping("/ReadAll")
    public ResponseEntity<ResponseDTO<String>> getAllCategoriesOverview() {
        categoryService.getAllCategoriesOrEmpty();
        return ResponseEntity.ok(ResponseDTO.success("đã tìm thấy "));
    }

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategoriesOrEmpty());
    }

    @PostMapping("/Update")
    public ResponseEntity<ResponseDTO<String>> updateCategory(@RequestBody CategoryUpdateDTO categoryUpdateDTO) {
        categoryService.updateCategory(categoryUpdateDTO.getId(), categoryUpdateDTO.getName());
        return ResponseEntity.ok(ResponseDTO.success("đã cặp nhật thành công"));
    }

    @PostMapping("/Delete")
    public ResponseEntity<ResponseDTO<String>> deleteCategory(@RequestBody CategoryDeleteDTO categoryDeleteDTO) {
        categoryService.deleteCategory(categoryDeleteDTO.getId());
        return ResponseEntity.ok(ResponseDTO.success("đã xóa thành công"));
    }
}
