package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Laptop");
        product = new Product(1L, "MacBook Pro", 2000.0, 10, "M3 chip", "img.jpg", "vid.mp4", category);
    }

    @Test
    @DisplayName("Tạo sản phẩm thành công khi danh mục tồn tại")
    void testCreateProduct_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.createProduct("MacBook Pro", 2000.0, 10, 1L);

        assertNotNull(result);
        assertEquals("MacBook Pro", result.getName());
        assertEquals(2000.0, result.getPrice());
        assertEquals(10, result.getStock());
        assertEquals(category, result.getCategory());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Tạo sản phẩm thất bại khi danh mục không tồn tại")
    void testCreateProduct_CategoryNotFound_ThrowsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.createProduct("MacBook Pro", 2000.0, 10, 99L);
        });

        assertTrue(exception.getMessage().contains("không tìm thấy danh mục"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Lấy danh sách sản phẩm phân trang thành công")
    void testGetAllProducts_Success() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Product> result = productService.getAllProducts(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Giảm số lượng tồn kho thành công")
    void testReduceStock_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.reduceStock(1L, 3);

        assertEquals(7, product.getStock());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Giảm tồn kho thất bại khi sản phẩm không tồn tại")
    void testReduceStock_ProductNotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            productService.reduceStock(999L, 2);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Giảm tồn kho thất bại khi số lượng trong kho không đủ")
    void testReduceStock_NotEnoughStock_ThrowsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            productService.reduceStock(1L, 20); // Kho chỉ có 10
        });

        assertTrue(exception.getMessage().contains("sản phẩm k đủ"));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Giảm tồn kho thất bại khi số lượng giảm <= 0")
    void testReduceStock_InvalidQuantity_ThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            productService.reduceStock(1L, 0);
        });
        assertThrows(RuntimeException.class, () -> {
            productService.reduceStock(1L, -5);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Tìm kiếm sản phẩm theo tên và giá tối thiểu")
    void testSearchProducts_Success() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.searchProduct(eq("MacBook"), eq(1000.0), any(Pageable.class)))
                .thenReturn(page);

        Page<Product> result = productService.searchProducts("MacBook", 1000.0, 10, 0);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
