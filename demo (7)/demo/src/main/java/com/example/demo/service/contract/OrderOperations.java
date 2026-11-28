package com.example.demo.service.contract;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;

import java.util.List;

/**
 * Hợp đồng vòng đời Đơn hàng (ISP: tách biệt khỏi OrderItem và Payment Webhook).
 */
public interface OrderOperations {

    Order placeOrder(List<OrderItem> items, String couponCode, String email, String idempotencyKey);

    Order updateOrderStatus(Long orderId, String targetStatusStr);

    Order cancelOrder(Long orderId, String requesterEmail, boolean isAdmin);

    Order getOrderForRequester(Long orderId, String requesterEmail, boolean isAdmin);

    Order getOrderById(Long id);

    List<Order> getOrdersByUserEmail(String email);

    int expireUnpaidOrders();
}
