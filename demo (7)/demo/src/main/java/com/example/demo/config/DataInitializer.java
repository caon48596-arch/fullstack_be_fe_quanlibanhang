package com.example.demo.config;

import com.example.demo.entity.Category;
import com.example.demo.Repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(CategoryRepository categoryRepository) {
        return args -> {
            List<String> defaultCategories = Arrays.asList(
                "Điện thoại",
                "Máy tính bảng",
                "Phụ kiện",
                "Laptop",
                "PC", 
                "Màn hình"
            );
            
            for (String catName : defaultCategories) {
                if (!categoryRepository.existsByName(catName)) {
                    Category category = new Category();
                    category.setName(catName);
                    categoryRepository.save(category);
                    System.out.println("Đã thêm danh mục mặc định: " + catName);
                }
            }

            // ĐÃ XÓA TOÀN BỘ CODE SEED SẢN PHẨM ẢO TỰ ĐỘNG.
            // Hệ thống chỉ lưu trữ và hiển thị sản phẩm do Admin tạo (create) hoặc cập nhật (update).
        };
    }
}

