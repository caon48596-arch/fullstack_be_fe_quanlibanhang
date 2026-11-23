package com.example.demo.Unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.entity.Category;
import com.example.demo.service.CategoryService;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category(1L, "Laptop");
    }

    @Test
    @DisplayName("Tạo danh mục thành công khi tên chưa tồn tại")
    void testCreate_Success() {
        when(categoryRepository.findByName("Laptop")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        Category result = categoryService.create("Laptop");

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(categoryRepository, times(1)).findByName("Laptop");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Tạo danh mục thất bại khi tên đã tồn tại")
    void testCreate_AlreadyExists_ThrowsException() {
        when(categoryRepository.findByName("Laptop")).thenReturn(Optional.of(sampleCategory));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.create("Laptop");
        });

        assertTrue(exception.getMessage().contains("đã tồn tại"));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Lấy danh mục theo tên thành công")
    void testGet_Success() {
        when(categoryRepository.findByName("Laptop")).thenReturn(Optional.of(sampleCategory));

        Category result = categoryService.get("Laptop");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
    }

    @Test
    @DisplayName("Lấy danh mục theo tên không tìm thấy")
    void testGet_NotFound_ThrowsException() {
        when(categoryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            categoryService.get("Unknown");
        });
    }

    @Test
    @DisplayName("Lấy tất cả danh mục thành công")
    void testGetAll_Success() {
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory));

        List<Category> result = categoryService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Lấy tất cả danh mục khi danh sách rỗng")
    void testGetAll_Empty_ThrowsException() {
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(RuntimeException.class, () -> {
            categoryService.getAll();
        });
    }

    @Test
    @DisplayName("Xóa danh mục thành công khi ID tồn tại")
    void testDelete_Success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.delete(1L);

        verify(categoryRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Xóa danh mục thất bại khi ID không tồn tại")
    void testDelete_NotFound_ThrowsException() {
        when(categoryRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.delete(999L);
        });

        assertTrue(exception.getMessage().contains("không tìm thấy"));
        verify(categoryRepository, never()).deleteById(999L);
    }

    @Test
    @DisplayName("Cập nhật danh mục thành công")
    void testUpdate_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        Category updated = categoryService.update(1L, "Laptop Gaming");

        assertNotNull(updated);
        assertEquals("Laptop Gaming", updated.getName());
        verify(categoryRepository, times(1)).save(sampleCategory);
    }

    @Test
    @DisplayName("Cập nhật danh mục thất bại khi ID không tồn tại")
    void testUpdate_NotFound_ThrowsException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            categoryService.update(999L, "New Name");
        });
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
