package com.example.demo.service.notification;

import com.example.demo.event.OrderPlacedEvent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Triển khai render giao diện HTML chuẩn cho Email OTP và Hóa đơn đơn hàng.
 */
@Component
public class HtmlEmailTemplateRenderer implements EmailTemplateRenderer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public RenderedEmail renderOtpEmail(String otp) {
        String subject = "[IPWORLD] Mã Xác Thực OTP Của Bạn";
        String content = "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>Xác thực tài khoản IPWORLD</h2>"
                + "<p>Mã xác thực OTP của bạn là:</p>"
                + "<h1 style=\"color: #007bff; letter-spacing: 5px;\">" + escapeHtml(otp) + "</h1>"
                + "<p>Mã này bao gồm 6 chữ số và có hiệu lực trong vòng 5 phút.</p>"
                + "<p style=\"color: red;\">Tuyệt đối không chia sẻ mã này cho bất kỳ ai để bảo mật tài khoản.</p>"
                + "</div>";
        return new RenderedEmail(subject, content, "OTP (" + otp + ")");
    }

    @Override
    public RenderedEmail renderOrderInvoiceEmail(OrderPlacedEvent event) {
        NumberFormat vndFormat = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        String orderDateText = event.orderDate() != null ? event.orderDate().format(DATE_FORMATTER) : "";

        StringBuilder rowsHtml = new StringBuilder();
        if (event.items() != null) {
            for (OrderPlacedEvent.OrderItemSnapshot item : event.items()) {
                BigDecimal unitPrice = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
                rowsHtml.append("<tr>")
                        .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0;\">")
                        .append(escapeHtml(item.productName()))
                        .append("</td>")
                        .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: center;\">")
                        .append(item.quantity())
                        .append("</td>")
                        .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: right;\">")
                        .append(vndFormat.format(unitPrice)).append("đ</td>")
                        .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold; color: #173B70;\">")
                        .append(vndFormat.format(lineTotal)).append("đ</td>")
                        .append("</tr>");
            }
        }

        String couponRow = (event.couponCode() != null && !event.couponCode().isBlank())
                ? "<p style=\"margin: 6px 0; font-size: 13px; color: #059669;\">Mã giảm giá đã áp dụng: <strong>"
                        + escapeHtml(event.couponCode()) + "</strong></p>"
                : "";

        BigDecimal totalAmount = event.totalAmount() != null ? event.totalAmount() : BigDecimal.ZERO;

        String subject = "[IPWORLD] Xác nhận hóa đơn đơn hàng #HD-" + event.orderId();
        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 620px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                + "<div style=\"background: #173B70; color: #ffffff; padding: 20px 24px;\">"
                + "<h2 style=\"margin: 0; font-size: 20px;\">HÓA ĐƠN ĐẶT HÀNG IPWORLD</h2>"
                + "<p style=\"margin: 4px 0 0; font-size: 13px; color: #bfdbfe;\">Mã hóa đơn: #HD-" + event.orderId() + " • " + orderDateText + "</p>"
                + "</div>"
                + "<div style=\"padding: 24px; color: #1e293b;\">"
                + "<p style=\"margin-top: 0;\">Xin chào <strong>" + escapeHtml(event.customerName()) + "</strong>,</p>"
                + "<p>Cảm ơn bạn đã mua sắm tại <strong>IPWORLD Store</strong>. Đơn hàng của bạn đã được tiếp nhận và đang chờ xác nhận.</p>"
                + "<table style=\"width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13px;\">"
                + "<thead><tr style=\"background: #f1f5f9;\">"
                + "<th style=\"padding: 10px; text-align: left;\">Sản phẩm</th>"
                + "<th style=\"padding: 10px; text-align: center;\">SL</th>"
                + "<th style=\"padding: 10px; text-align: right;\">Đơn giá</th>"
                + "<th style=\"padding: 10px; text-align: right;\">Thành tiền</th>"
                + "</tr></thead>"
                + "<tbody>" + rowsHtml + "</tbody>"
                + "</table>"
                + couponRow
                + "<div style=\"background: #f8fafc; padding: 14px 16px; border-radius: 8px; text-align: right;\">"
                + "<span style=\"font-size: 14px; font-weight: bold;\">TỔNG THANH TOÁN: </span>"
                + "<span style=\"font-size: 20px; font-weight: 900; color: #d70018;\">" + vndFormat.format(totalAmount) + "đ</span>"
                + "</div>"
                + "</div>"
                + "<div style=\"background: #f1f5f9; padding: 14px 24px; font-size: 12px; color: #64748b; text-align: center;\">"
                + "© 2026 IPWORLD Store • Email tự động gửi qua Transactional Outbox."
                + "</div>"
                + "</div>";

        return new RenderedEmail(subject, html, "Hóa đơn #HD-" + event.orderId());
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
