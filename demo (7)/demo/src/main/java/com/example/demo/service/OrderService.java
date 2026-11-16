package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.Coupon;
import com.example.demo.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public Order placeOrder(Long Id, List<OrderItem> items, String couponCode, String email) {
        // tạo đơn hàng
        Order order = new Order();
        order.setId(Id);
        order.setOrderDate(LocalDateTime.now());
        
        if (email != null && !email.isEmpty()) {
            userRepository.findByEmail(email).ifPresent(order::setUser);
        }

        // tính tổng và lưu danh sách chi tiết
        double totalAmount = items.stream()
                .mapToDouble(OrderItem -> {
                    Product product = productRepository.findByIdForUpdate(OrderItem.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("sản phẩm k tồn tại"));
                    
                    if (product.getStock() != null) {
                        if (product.getStock() < OrderItem.getQuantity()) {
                            throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ số lượng trong kho");
                        }
                        product.setStock(product.getStock() - OrderItem.getQuantity());
                        productRepository.save(product);
                    }

                    return product.getPrice() * OrderItem.getQuantity();
                })
                .sum();
                
        // Áp dụng mã giảm giá nếu có
        if (couponCode != null && !couponCode.isEmpty()) {
            Optional<Coupon> couponOpt = couponRepository.findByCodeForUpdate(couponCode);
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                if (coupon.isActive() && coupon.getQuantity() > 0) {
                    if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
                        throw new RuntimeException("Mã giảm giá đã hết hạn sử dụng");
                    }
                    totalAmount = totalAmount - coupon.getDiscountAmount();
                    if (totalAmount < 0) totalAmount = 0;
                    
                    coupon.setQuantity(coupon.getQuantity() - 1);
                    if (coupon.getQuantity() <= 0) {
                        coupon.setActive(false);
                    }
                    couponRepository.save(coupon);
                } else {
                    throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng hoặc không hợp lệ");
                }
            }
        }

        // lưu vào db
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        Order savedOrder = orderRepository.save(order);

        items.forEach(item -> {
            item.setOrder(savedOrder);
            // set price explicitly to avoid null if frontend doesn't map correctly
            if (item.getPrice() == null) {
                productRepository.findById(item.getProduct().getId())
                    .ifPresent(p -> item.setPrice(p.getPrice()));
            }
            orderItemRepository.save(item);
        });
        return savedOrder;
    }

    public Order ReadOne(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("không tìm thấy id sản phẩm"));
    }

    public List<OrderItem> ReadAll() {
        return orderItemRepository.findAll();
    }
    
    public List<Order> getOrdersByUserEmail(String email) {
        return orderRepository.findByUser_Email(email);
    }
    
    public List<OrderItem> getOrderItemsByUserEmail(String email) {
        return orderItemRepository.findByOrder_User_Email(email);
    }
    
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
    
    public OrderItem updateOrderItemStatus(Long itemId, String status) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục đơn hàng"));
        item.setStatus(status);
        return orderItemRepository.save(item);
    }
}
