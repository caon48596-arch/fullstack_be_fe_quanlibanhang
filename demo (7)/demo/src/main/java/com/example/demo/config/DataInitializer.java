package com.example.demo.config;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(CategoryRepository categoryRepository, ProductRepository productRepository) {
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

            // Đảm bảo mỗi loại sản phẩm (danh mục) có đúng 1 sản phẩm trong database
            seedSingleProductIfEmpty(categoryRepository, productRepository, "Điện thoại", 
                "iPhone 16 Pro Max 256GB Titan Tự Nhiên", 34990000.0, 30, 
                "Thiết kế Titan sa mạc sang trọng, camera 48MP và chip A18 Pro đỉnh cao.", 
                "/HINH_DIEN_THOAI/ip16pro.jpg", "/HINH_DIEN_THOAI/iPhone 16 Reveal (4K).mp4");

            seedSingleProductIfEmpty(categoryRepository, productRepository, "Máy tính bảng", 
                "iPad Pro M4 11 inch Wi-Fi 256GB Siêu Mỏng", 27990000.0, 25, 
                "Màn hình OLED Ultra Retina XDR, chip Apple M4 đột phá thiết kế siêu mỏng.", 
                "/HINH_MAY_TINH_BANG/ipad.jpg", "/HINH_MAY_TINH_BANG/The all-new iPad Pro  TV Ad  Thinpossible.mp4");

            seedSingleProductIfEmpty(categoryRepository, productRepository, "Phụ kiện", 
                "Tai Nghe AirPods Pro 2 MagSafe USB-C", 5890000.0, 50, 
                "Chống ồn chủ động gấp 2 lần, âm thanh Adaptive Audio cao cấp chính hãng Apple.", 
                "/HINH_PHU_KIEN/TAINGHE.JPG", "/HINH_PHU_KIEN/JBL - Go 4 - Ultra-Portable Bluetooth Speaker.mp4");

            seedSingleProductIfEmpty(categoryRepository, productRepository, "Laptop", 
                "MacBook Air M2 13.6 inch 256GB Chính Hãng", 24490000.0, 20, 
                "Thiết kế mỏng nhẹ, pin 18 tiếng, chip Apple M2 vượt trội.", 
                "/HINH_LAPTOP/MACBM2.JPG", "/HINH_LAPTOP/New MacBook Pro with PROLED XDR Display and Touch- The worlds best laptop display.mp4");

            seedSingleProductIfEmpty(categoryRepository, productRepository, "PC", 
                "PC Gaming CyberCore i7 RTX 4070 12GB", 32500000.0, 15, 
                "PC Gaming cấu hình cực mạnh Core i7 14700F, RAM 32GB, RTX 4070 12GB đồ họa mượt mà.", 
                "/HINH_PC/PCGamingRTX4070.jpg", "/HINH_PC/Dream PC build - RTX 5080 Noctua + ASUS ProArt Z890 + Core Ultra 9 285K - For Creators and Gaming (online-video-cutter.com).mp4");

            seedSingleProductIfEmpty(categoryRepository, productRepository, "Màn hình", 
                "Màn Hình Dell UltraSharp 27 inch 4K IPS U2723QE", 14890000.0, 18, 
                "Độ chuẩn màu 100% sRGB, tấm nền IPS Black hiển thị tương phản sắc nét tuyệt đỉnh.", 
                "/HINH_MAN_HINH/manhinh_dell.jpg", "/HINH_MAN_HINH/PRO MP242A E2 & MP271A E2 & MP275 E2 Series120Hz Home Office Monitors in Various Sizes - MSI.mp4");
        };
    }

    private void seedSingleProductIfEmpty(CategoryRepository categoryRepository, ProductRepository productRepository,
                                          String catName, String prodName, Double price, Integer stock,
                                          String description, String imageUrl, String videoUrl) {
        Category cat = categoryRepository.findByName(catName).orElse(null);
        if (cat != null) {
            List<Product> products = productRepository.findByCategoryId(cat.getId());
            if (products.isEmpty()) {
                Product p = new Product(null, prodName, price, stock, description, imageUrl, videoUrl, cat);
                productRepository.save(p);
                System.out.println("Đã khởi tạo 1 sản phẩm cho danh mục: " + catName);
            }
        }
    }
}
