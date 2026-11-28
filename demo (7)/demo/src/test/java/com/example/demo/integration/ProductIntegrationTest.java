package com.example.demo.integration;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Product Integration Tests")
public class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
        }

        if (userRepository.findByEmail("client@gmail.com").isEmpty()) {
            User client = new User();
            client.setEmail("client@gmail.com");
            client.setPassword(passwordEncoder.encode("client123"));
            client.setRole("ROLE_CLIENT");
            userRepository.save(client);
        }
    }

    @Test
    @DisplayName("Should retrieve all products successfully without authentication")
    void testGetAllProducts_Success() throws Exception {
        Category category = new Category();
        category.setName("Điện thoại");
        categoryRepository.save(category);

        Product p1 = new Product();
        p1.setName("Iphone 15");
        p1.setCategory(category);
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setName("Samsung S24");
        p2.setCategory(category);
        productRepository.save(p2);

        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Should retrieve single product details by ID successfully")
    void testGetProductById_Success() throws Exception {
        Category category = new Category();
        category.setName("Máy tính");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("MacBook Pro");
        product.setPrice(new BigDecimal("50000000.00"));
        product.setCategory(category);
        productRepository.save(product);

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("MacBook Pro")));
    }

    @Test
    @DisplayName("Should allow ADMIN to create a new product successfully")
    void testAdminCreateProduct_Success() throws Exception {
        Category category = new Category();
        category.setName("Âm thanh");
        categoryRepository.save(category);

        String token = jwtUtil.generateToken("admin@gmail.com", "ROLE_ADMIN");

        String requestBody = "{"
                + "\"name\":\"Tai nghe Bluetooth\","
                + "\"price\":500000.0,"
                + "\"stock\":20,"
                + "\"category\":{\"id\":" + category.getId() + "}"
                + "}";

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        boolean exists = productRepository.findAll()
                .stream()
                .anyMatch(p -> "Tai nghe Bluetooth".equals(p.getName()));
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should allow ADMIN to update an existing product")
    void testAdminUpdateProduct_Success() throws Exception {
        Category category = new Category();
        category.setName("Phụ kiện");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Sản phẩm cũ");
        product.setPrice(new BigDecimal("100000.00"));
        product.setStock(5);
        product.setCategory(category);
        productRepository.save(product);

        String token = jwtUtil.generateToken("admin@gmail.com", "ROLE_ADMIN");

        String requestBody = "{"
                + "\"name\":\"Sản phẩm đã cập nhật\","
                + "\"price\":250000.00"
                + "}";

        mockMvc.perform(put("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("250000.00").compareTo(updated.getPrice()));
        assertEquals("Sản phẩm đã cập nhật", updated.getName());
    }

    @Test
    @DisplayName("Should allow ADMIN to delete product and return 204 No Content")
    void testAdminDeleteProduct_Success() throws Exception {
        Product product = new Product();
        product.setName("Sản phẩm cần xóa");
        product.setPrice(new BigDecimal("99000.00"));
        product.setStock(3);
        productRepository.save(product);
        Long productId = product.getId();

        String token = jwtUtil.generateToken("admin@gmail.com", "ROLE_ADMIN");

        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertTrue(productRepository.findById(productId).orElseThrow().isDeleted());
        assertTrue(productRepository.findByIdAndDeletedFalse(productId).isEmpty());
    }

    @Test
    @DisplayName("Should forbid CLIENT from creating a new product (403 Forbidden)")
    void testClientCreateProduct_ShouldReturnForbidden() throws Exception {
        String tokenClient = jwtUtil.generateToken("client@gmail.com", "ROLE_CLIENT");

        String requestBody = "{\"name\":\"San pham lau\",\"price\":100.0,\"stock\":1}";

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + tokenClient)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should forbid CLIENT from deleting a product (403 Forbidden)")
    void testClientDeleteProduct_ShouldReturnForbidden() throws Exception {
        Product product = new Product();
        product.setName("Sản phẩm không được phép xóa");
        product.setPrice(new BigDecimal("50000.00"));
        product.setStock(10);
        productRepository.save(product);

        String tokenClient = jwtUtil.generateToken("client@gmail.com", "ROLE_CLIENT");

        mockMvc.perform(delete("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + tokenClient))
                .andExpect(status().isForbidden());
    }
}
