package com.example.demo.Api_test;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API Test - Category Service")
public class CategoryApiTest extends BaseApiTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category sampleCategory;

    @BeforeEach
    void setUpCategory() {
        sampleCategory = new Category();
        sampleCategory.setName("Danh Mục Mau " + System.currentTimeMillis());
        sampleCategory = categoryRepository.save(sampleCategory);
    }

    @Test
    @DisplayName("GET /api/categories - Lấy danh sách toàn bộ danh mục (Public, không cần token)")
    void testGetAllCategories_Public() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("GET /api/categories/ReadAll - Lấy tổng quan trạng thái danh mục")
    void testGetAllCategoriesOverview() throws Exception {
        mockMvc.perform(get("/api/categories/ReadAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("GET /api/categories/Read - Tìm danh mục theo tên thành công")
    void testGetCategoryByName_Success() throws Exception {
        Map<String, String> body = Map.of("name", sampleCategory.getName());

        mockMvc.perform(get("/api/categories/Read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("POST /api/categories/Create - ADMIN tạo mới danh mục thành công")
    void testCreateCategory_AdminSuccess() throws Exception {
        Map<String, String> body = Map.of("name", "Danh Mục Độc Nhất " + System.currentTimeMillis());

        mockMvc.perform(post("/api/categories/Create")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("POST /api/categories/Create - CLIENT bị từ chối quyền tạo danh mục (403 Forbidden)")
    void testCreateCategory_ClientForbidden() throws Exception {
        Map<String, String> body = Map.of("name", "Category Bi Tu Choi");

        mockMvc.perform(post("/api/categories/Create")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/categories/Update - ADMIN cập nhật tên danh mục thành công")
    void testUpdateCategory_AdminSuccess() throws Exception {
        Map<String, Object> body = Map.of(
                "id", sampleCategory.getId(),
                "name", "Danh Mục Đã Đổi Tên " + System.currentTimeMillis()
        );

        mockMvc.perform(post("/api/categories/Update")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("POST /api/categories/Delete - ADMIN xóa danh mục theo ID thành công")
    void testDeleteCategory_AdminSuccess() throws Exception {
        Category catToDelete = new Category();
        catToDelete.setName("Xóa Tạm " + System.currentTimeMillis());
        catToDelete = categoryRepository.save(catToDelete);

        Map<String, Object> body = Map.of("id", catToDelete.getId());

        mockMvc.perform(post("/api/categories/Delete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }
}
