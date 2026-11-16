package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Order;

import jakarta.transaction.Transactional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Transactional
    @Query("Update Order o Set o.totalAmount = ?2 Where o.id = ?1")
    void setTotalAmount(long id, double amount);

    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double sumTotalRevenue();

    java.util.List<Order> findByUser_Email(String email);

    @Query(value = "SELECT DATE(order_date) as date, COALESCE(SUM(total_amount), 0) as revenue, COUNT(id) as orderCount FROM orders GROUP BY DATE(order_date) ORDER BY date DESC", nativeQuery = true)
    java.util.List<com.example.demo.dto.DailyRevenue> getDailyRevenue();

    java.util.List<Order> findTop10ByOrderByOrderDateDesc();
}
