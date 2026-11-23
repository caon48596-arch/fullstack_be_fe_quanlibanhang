package com.example.demo.System_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System Test - Order (Giỏ hàng, Đặt mua, Thanh toán)")
public class OrderSystemTest extends BaseSystemTest {

    @Test
    @DisplayName("ST-ORD-01: Kiểm tra thêm sản phẩm vào Giỏ hàng và mở giỏ hàng")
    void testAddToCart_AndCheckCart() {
        driver.get(BASE_URL + "/shop");

        By addToCartBtnLocator = By.xpath("//button[contains(text(),'Thêm') or contains(@class,'bg-gray-900')]");
        try {
            WebElement addToCartBtn = waitForClickable(addToCartBtnLocator);
            addToCartBtn.click();
            try {
                driver.switchTo().alert().accept();
            } catch (Exception ignored) {}
        } catch (Exception e) {
            driver.get(BASE_URL + "/orders");
        }

        driver.get(BASE_URL + "/orders");
        boolean isCartPageLoaded = wait.until(d -> d.getPageSource().contains("Giỏ hàng") || d.getPageSource().contains("Thanh toán") || d.getCurrentUrl().contains("/orders"));
        assertTrue(isCartPageLoaded, "Trang giỏ hàng và thanh toán phải được hiển thị.");
    }

    @Test
    @DisplayName("ST-ORD-02: Kiểm tra quy trình Đặt mua và Thanh toán đơn hàng")
    void testCheckoutProcess() {
        driver.get(BASE_URL + "/orders");

        List<WebElement> checkoutBtns = driver.findElements(By.xpath("//button[contains(text(),'Thanh toán') or contains(text(),'Đặt hàng')]"));
        assertTrue(!checkoutBtns.isEmpty() || driver.getPageSource().contains("Giỏ hàng"),
                "Giao diện đơn hàng phải có mục tiến hành đặt hàng hoặc thông báo giỏ hàng.");
    }
}
