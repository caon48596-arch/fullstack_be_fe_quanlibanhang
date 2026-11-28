package com.example.demo.controller;

import com.example.demo.dto.OrderDTO.OrderCreateandSumDTO;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.contract.OrderItemOperations;
import com.example.demo.service.contract.OrderOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderOperations orderService;
    private final OrderItemOperations orderItemService;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
    }

    @PostMapping({"", "/CreateAndSum"})
    public ResponseEntity<ResponseDTO<Order>> createAndSum(
            @RequestBody OrderCreateandSumDTO orderCreateandSumDTO,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
            Principal principal) {
        String userEmail = (principal != null && principal.getName() != null)
                ? principal.getName()
                : orderCreateandSumDTO.getEmail();

        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new UnauthorizedException("Quý khách vui lòng đăng nhập để thanh toán và tạo hóa đơn.");
        }

        String idempotencyKey = (idempotencyHeader != null && !idempotencyHeader.isBlank())
                ? idempotencyHeader
                : orderCreateandSumDTO.getIdempotencyKey();

        Order savedOrder = orderService.placeOrder(
                orderCreateandSumDTO.getItems(),
                orderCreateandSumDTO.getCouponCode(),
                userEmail,
                idempotencyKey
        );

        return ResponseEntity.ok(ResponseDTO.success(savedOrder));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<Order>> getMyOrders(
            @RequestParam(required = false) String email,
            Principal principal) {
        String userEmail = (principal != null && principal.getName() != null) ? principal.getName() : email;
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BusinessException("Thiếu email người dùng");
        }
        return ResponseEntity.ok(orderService.getOrdersByUserEmail(userEmail));
    }

    @GetMapping("/my-order-items")
    public ResponseEntity<List<OrderItem>> getMyOrderItems(
            @RequestParam(required = false) String email,
            Principal principal) {
        String userEmail = (principal != null && principal.getName() != null) ? principal.getName() : email;
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BusinessException("Thiếu email người dùng");
        }
        return ResponseEntity.ok(orderItemService.getOrderItemsByUserEmail(userEmail));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseDTO<Order>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        Order updated = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ResponseDTO.success(updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ResponseDTO<Order>> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        Order cancelled = orderService.cancelOrder(id, email, isAdmin(authentication));
        return ResponseEntity.ok(ResponseDTO.success(cancelled));
    }

    @PutMapping("/item/{id}/status")
    public ResponseEntity<ResponseDTO<String>> updateItemStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        orderItemService.updateOrderItemStatus(id, status);
        return ResponseEntity.ok(ResponseDTO.success("Cập nhật trạng thái sản phẩm thành công"));
    }

    @GetMapping({"/{id}", "/ReadOne/{id}"})
    public ResponseEntity<ResponseDTO<Order>> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        Order order = orderService.getOrderForRequester(id, email, isAdmin(authentication));
        return ResponseEntity.ok(ResponseDTO.success(order));
    }

    @GetMapping({"", "/ReadAll"})
    public ResponseEntity<List<OrderItem>> getAllOrderItems() {
        return ResponseEntity.ok(orderItemService.getAllOrderItems());
    }
}
