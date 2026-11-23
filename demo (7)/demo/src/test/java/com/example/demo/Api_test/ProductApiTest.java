package com.example.demo.Api_test;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API Test - Product Service")
public class ProductApiTest extends BaseApiTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category sampleCategory;
    private Product sampleProduct;

    @BeforeEach
    void setUpProduct() {
        sampleCategory = new Category();
        sampleCategory.setName("Category Cho Product " + System.currentTimeMillis());
        sampleCategory = categoryRepository.save(sampleCategory);

        sampleProduct = new Product();
        sampleProduct.setName("Sản Phẩm Test Service");
        sampleProduct.setPrice(250000.0);
        sampleProduct.setStock(30);
        sampleProduct.setCategory(sampleCategory);
        sampleProduct = productRepository.save(sampleProduct);
    }

    @Test
    @DisplayName("GET /api/products - Lấy danh sách tất cả sản phẩm (Public)")
    void testGetAllProducts_Public() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Lấy chi tiết sản phẩm hợp lệ")
    void testGetProductById_Success() throws Exception {
        mockMvc.perform(get("/api/products/" + sampleProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(sampleProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is(sampleProduct.getName())))
                .andExpect(jsonPath("$.price", is(250000.0)));
    }

    @Test
    @DisplayName("GET /api/products/{id} - Trả về 404 khi sản phẩm không tồn tại")
    void testGetProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/products/category/{categoryId} - Lấy danh sách sản phẩm theo danh mục")
    void testGetProductsByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/products/category/" + sampleCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("POST /api/products - ADMIN thêm mới sản phẩm thành công")
    void testCreateProduct_AdminSuccess() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Tai Nghe Gaming Chống Ồn");
        body.put("price", 1200000.0);
        body.put("stock", 15);
        body.put("category", Map.of("id", sampleCategory.getId()));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Tai Nghe Gaming Chống Ồn")))
                .andExpect(jsonPath("$.stock", is(15)));
    }

    @Test
    @DisplayName("POST /api/products - CLIENT bị chặn khi cố gắng thêm sản phẩm (403 Forbidden)")
    void testCreateProduct_ClientForbidden() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Sản Phẩm Lậu",
                "price", 100.0
        );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/products/{id} - ADMIN cập nhật thông tin sản phẩm thành công")
    void testUpdateProduct_AdminSuccess() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Sản Phẩm Đã Được Cập Nhật",
                "price", 299000.0,
                "stock", 50
        );

        mockMvc.perform(put("/api/products/" + sampleProduct.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Sản Phẩm Đã Được Cập Nhật")))
                .andExpect(jsonPath("$.price", is(299000.0)))
                .andExpect(jsonPath("$.stock", is(50)));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - ADMIN xóa sản phẩm (Trả về 204 No Content)")
    void testDeleteProduct_AdminSuccess() throws Exception {
        Product p = new Product();
        p.setName("Sản phẩm cần xóa ngay");
        p.setPrice(50000.0);
        p.setStock(2);
        p = productRepository.save(p);

        mockMvc.perform(delete("/api/products/" + p.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertTrue(productRepository.findById(p.getId()).isEmpty());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} - CLIENT bị chặn khi xóa sản phẩm (403 Forbidden)")
    void testDeleteProduct_ClientForbidden() throws Exception {
        mockMvc.perform(delete("/api/products/" + sampleProduct.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
