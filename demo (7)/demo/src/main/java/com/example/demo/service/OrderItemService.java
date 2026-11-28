package com.example.demo.service;

import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.entity.OrderItem;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.contract.OrderItemOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Tách riêng trách nhiệm quản lý OrderItem khỏi OrderItemController và OrderService (SRP & ISP).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderItemService implements OrderItemOperations {

    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByUserEmail(String email) {
        return orderItemRepository.findByOrder_User_Email(email);
    }

    @Override
    public OrderItem updateOrderItemStatus(Long itemId, String status) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục đơn hàng"));
        item.setStatus(status);
        return orderItemRepository.save(item);
    }

    @Override
    public Optional<OrderItem> updateOrderItem(Long id, OrderItem updatedItem) {
        return orderItemRepository.findById(id).map(item -> {
            if (updatedItem.getQuantity() != null) {
                item.setQuantity(updatedItem.getQuantity());
            }
            if (updatedItem.getProduct() != null) {
                item.setProduct(updatedItem.getProduct());
            }
            return orderItemRepository.save(item);
        });
    }

    @Override
    public boolean deleteOrderItem(Long id) {
        if (orderItemRepository.existsById(id)) {
            orderItemRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
