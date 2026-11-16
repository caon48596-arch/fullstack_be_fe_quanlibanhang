package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.entity.OrderItem;

import java.util.Optional;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {
    
    @Autowired
    private OrderItemRepository orderItemRepository;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Long id) {
        if (orderItemRepository.existsById(id)) {
            orderItemRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderItem> updateOrderItem(@PathVariable Long id, @RequestBody OrderItem updatedItem) {
        Optional<OrderItem> itemOpt = orderItemRepository.findById(id);
        if (itemOpt.isPresent()) {
            OrderItem item = itemOpt.get();
            if (updatedItem.getQuantity() != null) {
                item.setQuantity(updatedItem.getQuantity());
            }
            if (updatedItem.getProduct() != null) {
                item.setProduct(updatedItem.getProduct());
            }
            return ResponseEntity.ok(orderItemRepository.save(item));
        }
        return ResponseEntity.notFound().build();
    }
}
