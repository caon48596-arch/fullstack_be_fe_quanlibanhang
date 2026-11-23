package com.example.demo.integration;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkout Integration Tests")
public class CheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should place order successfully and deduct product stock")
    void testCheckout_Success_AndDeductsStock() throws Exception {
        User user = new User();
        user.setEmail("khachhang_checkout@gmail.com");
        user.setPassword(passwordEncoder.encode("matkhau123"));
        user.setRole("ROLE_CLIENT");
        userRepository.save(user);

        Category category = new Category();
        category.setName("Điện tử");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Điện thoại");
        product.setStock(10);
        product.setPrice(5000.0);
        product.setCategory(category);
        productRepository.save(product);

        String token = jwtUtil.generateToken("khachhang_checkout@gmail.com", "ROLE_CLIENT");

        String requestBody = "{\"email\":\"khachhang_checkout@gmail.com\","
                + "\"items\":[{\"product\":{\"id\":" + product.getId() + "},\"quantity\":2}]}";

        long orderCountBefore = orderRepository.count();

        mockMvc.perform(post("/api/orders/CreateAndSum")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(8, updatedProduct.getStock());

        long orderCountAfter = orderRepository.count();
        assertTrue(orderCountAfter > orderCountBefore, "Order must be saved into DB");
    }

    @Test
    @DisplayName("Should reject checkout request when authentication token is missing")
    void testCheckout_WithoutToken_ShouldBeRejected() throws Exception {
        Category category = new Category();
        category.setName("Điện tử");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Laptop Test");
        product.setStock(5);
        product.setPrice(10000.0);
        product.setCategory(category);
        productRepository.save(product);

        String requestBody = "{\"email\":\"khachhang_checkout@gmail.com\","
                + "\"items\":[{\"product\":{\"id\":" + product.getId() + "},\"quantity\":1}]}";

        mockMvc.perform(post("/api/orders/CreateAndSum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when requested quantity exceeds available stock")
    void testCheckout_ExceedingStock_ShouldReturnBadRequest() throws Exception {
        User user = new User();
        user.setEmail("khachhang_checkout2@gmail.com");
        user.setPassword(passwordEncoder.encode("matkhau123"));
        user.setRole("ROLE_CLIENT");
        userRepository.save(user);

        Category category = new Category();
        category.setName("Phụ kiện");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Sản phẩm ít hàng");
        product.setStock(2);
        product.setPrice(300.0);
        product.setCategory(category);
        productRepository.save(product);

        String token = jwtUtil.generateToken("khachhang_checkout2@gmail.com", "ROLE_CLIENT");

        String requestBody = "{\"email\":\"khachhang_checkout2@gmail.com\","
                + "\"items\":[{\"product\":{\"id\":" + product.getId() + "},\"quantity\":999}]}";

        mockMvc.perform(post("/api/orders/CreateAndSum")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
