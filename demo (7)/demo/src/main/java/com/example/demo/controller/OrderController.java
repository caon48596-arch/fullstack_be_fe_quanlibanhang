package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OrderDTO.OrderCreateandSumDTO;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/CreateAndSum")
    public ResponseEntity<ResponseDTO<?>> createAndSum(
            @RequestBody OrderCreateandSumDTO orderCreateandSumDTO,
            Principal principal) {
        try {
            // Bảo mật: Bắt buộc người dùng phải đăng nhập mới được thanh toán và tạo hóa đơn
            String userEmail = (principal != null && principal.getName() != null)
                    ? principal.getName()
                    : orderCreateandSumDTO.getEmail();

            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.status(401).body(ResponseDTO.error("Quý khách vui lòng đăng nhập để thanh toán và tạo hóa đơn.", "401"));
            }

            Order savedOrder = orderService.placeOrder(
                orderCreateandSumDTO.getId(), 
                orderCreateandSumDTO.getItems(), 
                orderCreateandSumDTO.getCouponCode(),
                userEmail
            );

            return ResponseEntity.ok(ResponseDTO.success(savedOrder));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ResponseDTO.error(e.getMessage() != null ? e.getMessage() : "Không thể tạo hóa đơn", "400"));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestParam(required = false) String email,
            Principal principal) {
        try {
            // Bảo mật: Bắt buộc lấy email từ JWT của người dùng đã xác thực nếu có
            String userEmail = (principal != null && principal.getName() != null) ? principal.getName() : email;
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(orderService.getOrdersByUserEmail(userEmail));
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
    }

    @GetMapping("/my-order-items")
    public ResponseEntity<List<OrderItem>> getMyOrderItems(
            @RequestParam(required = false) String email,
            Principal principal) {
        try {
            // Bảo mật: Bắt buộc lấy email từ JWT của người dùng đã xác thực nếu có
            String userEmail = (principal != null && principal.getName() != null) ? principal.getName() : email;
            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(orderService.getOrderItemsByUserEmail(userEmail));
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseDTO<String>> updateStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        try {
            orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(ResponseDTO.success("Cập nhật trạng thái thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ResponseDTO.error(e.getMessage(), "400"));
        }
    }

    @PutMapping("/item/{id}/status")
    public ResponseEntity<ResponseDTO<String>> updateItemStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        try {
            orderService.updateOrderItemStatus(id, status);
            return ResponseEntity.ok(ResponseDTO.success("Cập nhật trạng thái sản phẩm thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ResponseDTO.error(e.getMessage(), "400"));
        }
    }

    // 2. Xem chi tiết 1 đơn hàng theo ID
    @GetMapping("/ReadOne/{id}")
    public ResponseEntity<ResponseDTO<String>> readOne(@PathVariable Long id) {
        try {
            orderService.ReadOne(id);
            return ResponseEntity.ok(ResponseDTO.success("Hiển thị thành công đơn hàng"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ResponseDTO.error("Không tìm thấy đơn hàng", "404"));
        }
    }

    // 3. Xem danh sách tất cả các món hàng đã đặt (Chỉ Admin)
    @GetMapping("/ReadAll")
    public ResponseEntity<List<OrderItem>> readAll() {
        try {
            return ResponseEntity.ok(orderService.ReadAll());
        } catch (Exception e) {
            return ResponseEntity.status(403).build();
        }
    }
}
