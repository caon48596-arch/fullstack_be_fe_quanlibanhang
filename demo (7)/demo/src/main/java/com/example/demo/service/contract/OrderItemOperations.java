package com.example.demo.service.contract;

import com.example.demo.entity.OrderItem;

import java.util.List;
import java.util.Optional;

/**
 * Hợp đồng quản lý Chi tiết mục đơn hàng (OrderItem) (ISP & SRP).
 */
public interface OrderItemOperations {

    List<OrderItem> getAllOrderItems();

    List<OrderItem> getOrderItemsByUserEmail(String email);

    OrderItem updateOrderItemStatus(Long itemId, String status);

    Optional<OrderItem> updateOrderItem(Long id, OrderItem updatedItem);

    boolean deleteOrderItem(Long id);
}
