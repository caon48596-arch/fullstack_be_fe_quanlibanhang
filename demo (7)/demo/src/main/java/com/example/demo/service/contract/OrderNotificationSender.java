package com.example.demo.service.contract;

import com.example.demo.event.OrderPlacedEvent;

/**
 * Interface chuyên biệt cho việc gửi thông báo hóa đơn đặt hàng (ISP & DIP).
 */
public interface OrderNotificationSender {

    void sendOrderInvoiceEmail(OrderPlacedEvent event);
}
