package com.example.demo.System_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("System Test - Permission (Chặn khách truy cập trái phép trang Admin)")
public class PermissionSystemTest extends BaseSystemTest {

    @Test
    @DisplayName("ST-PERM-01: Khách chưa đăng nhập truy cập /admin bị chặn và từ chối quyền")
    void testUnauthenticatedUser_AccessAdmin_ShouldBeBlocked() {
        driver.get(BASE_URL + "/");
        clearAuthTokenInStorage();

        driver.get(BASE_URL + "/admin");

        boolean accessBlocked = wait.until(d ->
                d.getPageSource().contains("Truy cập bị từ chối") ||
                d.getPageSource().contains("Từ chối") ||
                d.getCurrentUrl().contains("/login") ||
                d.getCurrentUrl().equals(BASE_URL + "/")
        );

        assertTrue(accessBlocked, "Khách chưa đăng nhập bắt buộc phải bị chặn khi cố truy cập trang Admin.");
    }

    @Test
    @DisplayName("ST-PERM-02: Tài khoản Khách hàng (ROLE_CLIENT) truy cập /admin bị từ chối quyền")
    void testClientRole_AccessAdmin_ShouldBeBlocked() {
        driver.get(BASE_URL + "/login");

        // Giả lập JWT token với quyền ROLE_CLIENT
        setAuthTokenInStorage("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjbGllbnRAZ21haWwuY29tIiwicm9sZSI6IlJPTEVfQ0xJRU5UIn0.dummy_client_signature");

        driver.get(BASE_URL + "/admin");

        boolean clientBlocked = wait.until(d ->
                d.getPageSource().contains("Truy cập bị từ chối") ||
                d.getCurrentUrl().contains("/login") ||
                d.getCurrentUrl().equals(BASE_URL + "/")
        );

        assertTrue(clientBlocked, "Tài khoản ROLE_CLIENT không được phép vào trang Admin.");
    }
}
