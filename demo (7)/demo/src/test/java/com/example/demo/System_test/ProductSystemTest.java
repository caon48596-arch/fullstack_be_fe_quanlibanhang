package com.example.demo.System_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System Test - Product (Xem và Tìm kiếm sản phẩm)")
public class ProductSystemTest extends BaseSystemTest {

    @Test
    @DisplayName("ST-PROD-01: Kiểm tra xem danh sách sản phẩm tại trang cửa hàng")
    void testViewProductCatalog() {
        driver.get(BASE_URL + "/shop");

        waitForVisibility(By.xpath("//h1[contains(text(),'Cửa Hàng') or contains(text(),'Sản Phẩm')] | //div[contains(@class,'grid')]"));

        List<WebElement> productCards = driver.findElements(By.xpath("//div[contains(@class,'rounded-2xl')] | //div[contains(@class,'card')]"));
        assertFalse(productCards.isEmpty(), "Trang cửa hàng phải hiển thị ít nhất một thẻ sản phẩm.");
    }

    @Test
    @DisplayName("ST-PROD-02: Kiểm tra chức năng Tìm kiếm sản phẩm qua thanh tìm kiếm")
    void testSearchProducts() {
        driver.get(BASE_URL + "/");

        By searchInputLocator = By.xpath("//input[@placeholder or @type='text' or @name='search']");
        WebElement searchInput = waitForVisibility(searchInputLocator);

        String searchKeyword = "iPhone";
        searchInput.sendKeys(searchKeyword);
        searchInput.sendKeys(Keys.ENTER);

        boolean urlHasSearchParam = wait.until(d -> d.getCurrentUrl().contains("search="));
        assertTrue(urlHasSearchParam, "URL phải chứa param search sau khi thực hiện tìm kiếm.");
    }
}
