package com.example.demo.Repository;

import com.example.demo.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT u FROM Product u WHERE u.deleted = false AND u.name LIKE %?1% AND u.price >= ?2")
    Page<Product> searchProduct(String name, BigDecimal minprice, Pageable pageable);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByDeletedFalseOrderByIdDesc();

    Optional<Product> findByIdAndDeletedFalse(Long id);

    List<Product> findByCategoryIdAndDeletedFalseOrderByIdDesc(Long categoryId);

    Page<Product> findByDeletedFalse(Pageable pageable);

    long countByDeletedFalse();

    @Query("SELECT p FROM Product p WHERE p.deleted = false " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findFilteredProducts(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    /**
     * Tận dụng MySQL FULLTEXT INDEX `ft_products_name_desc(name, description)` để tìm kiếm nhanh
     * thay vì `LIKE '%keyword%'` gây full table scan trên tập sản phẩm lớn.
     */
    @Query(value = "SELECT * FROM products p WHERE p.deleted = false " +
            "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
            "AND MATCH(p.name, p.description) AGAINST(:booleanQuery IN BOOLEAN MODE) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
            countQuery = "SELECT COUNT(*) FROM products p WHERE p.deleted = false " +
            "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
            "AND MATCH(p.name, p.description) AGAINST(:booleanQuery IN BOOLEAN MODE) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice)",
            nativeQuery = true)
    Page<Product> findFilteredProductsFullText(
            @Param("categoryId") Long categoryId,
            @Param("booleanQuery") String booleanQuery,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
