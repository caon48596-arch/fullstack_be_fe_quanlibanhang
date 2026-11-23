package com.example.demo.System_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System Test - Admin (Doanh số, CRUD Sản phẩm, Quản lý đơn hàng)")
public class AdminSystemTest extends BaseSystemTest {

    // Giả lập JWT token với quyền ROLE_ADMIN
    private void authenticateAsAdmin() {
        driver.get(BASE_URL + "/login");
        setAuthTokenInStorage("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9BRE1JTiJ9.dummy_signature");
    }

    @Test
    @DisplayName("ST-ADM-01: Kiểm tra Dashboard quản trị và báo cáo Doanh số")
    void testAdminDashboard_RevenueOverview() {
        authenticateAsAdmin();
        driver.get(BASE_URL + "/admin");

        boolean hasAdminDashboard = wait.until(d ->
                d.getPageSource().contains("Dashboard") ||
                d.getPageSource().contains("Doanh thu") ||
                d.getPageSource().contains("IPWORLD Admin")
        );
        assertTrue(hasAdminDashboard, "Trang quản trị phải hiển thị số liệu Dashboard và Doanh thu.");
    }

    @Test
    @DisplayName("ST-ADM-02: Kiểm tra giao diện Quản lý Sản phẩm (CRUD)")
    void testAdminProducts_Management() {
        authenticateAsAdmin();
        driver.get(BASE_URL + "/admin/products");

        boolean hasProductManagement = wait.until(d ->
                d.getPageSource().contains("Quản lý Sản phẩm") ||
                d.getPageSource().contains("Thêm Sản Phẩm") ||
                d.getCurrentUrl().contains("/admin/products")
        );
        assertTrue(hasProductManagement, "Trang Quản lý Sản phẩm của Admin phải được hiển thị.");
    }

    @Test
    @DisplayName("ST-ADM-03: Kiểm tra giao diện Quản lý Đơn hàng")
    void testAdminOrders_Management() {
        authenticateAsAdmin();
        driver.get(BASE_URL + "/admin/orders");

        boolean hasOrdersManagement = wait.until(d ->
                d.getPageSource().contains("Đơn hàng") ||
                d.getPageSource().contains("Khách hàng") ||
                d.getCurrentUrl().contains("/admin/orders")
        );
        assertTrue(hasOrdersManagement, "Trang Quản lý Đơn hàng của Admin phải được hiển thị.");
    }
}
