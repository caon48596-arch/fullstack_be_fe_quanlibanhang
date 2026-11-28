package com.example.demo.Repository;

import com.example.demo.dto.DailyRevenue;
import com.example.demo.entity.Order;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByPaymentTransactionId(String paymentTransactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    List<Order> findByStatusAndExpiresAtBefore(String status, LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("Update Order o Set o.totalAmount = ?2 Where o.id = ?1")
    void setTotalAmount(long id, BigDecimal amount);

    // Loại trừ đơn hàng đã hủy (CANCELLED) hoặc hết hạn thanh toán (EXPIRED) khỏi doanh thu
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IS NULL OR o.status NOT IN ('CANCELLED', 'EXPIRED')")
    BigDecimal sumTotalRevenue();

    List<Order> findByUser_Email(String email);

    @Query(value = "SELECT DATE(order_date) as date, COALESCE(SUM(total_amount), 0) as revenue, COUNT(id) as orderCount " +
            "FROM orders WHERE status IS NULL OR status NOT IN ('CANCELLED', 'EXPIRED') " +
            "GROUP BY DATE(order_date) ORDER BY date DESC", nativeQuery = true)
    List<DailyRevenue> getDailyRevenue();

    List<Order> findTop10ByOrderByOrderDateDesc();
}
