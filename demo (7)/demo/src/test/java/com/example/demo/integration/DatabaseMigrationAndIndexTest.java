package com.example.demo.integration;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Coupon;
import com.example.demo.entity.User;
import jakarta.transaction.Transactional;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Database Schema Migration (Flyway) & Indexing Verification")
public class DatabaseMigrationAndIndexTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponRepository couponRepository;

    private Set<String> getTableIndexes(Connection conn, String tableName) throws Exception {
        Set<String> indexes = new HashSet<>();
        DatabaseMetaData meta = conn.getMetaData();
        for (String candidate : new String[]{tableName.toUpperCase(), tableName.toLowerCase()}) {
            try (ResultSet rs = meta.getIndexInfo(null, null, candidate, false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        indexes.add(indexName.toLowerCase());
                    }
                }
            }
        }
        return indexes;
    }

    @Test
    @DisplayName("Flyway đã áp dụng đầy đủ V1 (Init Schema) và V2 (Indexes & Constraints)")
    void testFlywayAppliedAllMigrations() {
        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 2, "Phải có ít nhất 2 migration (V1 và V2) đã được áp dụng");

        Set<String> versions = new HashSet<>();
        for (MigrationInfo info : applied) {
            if (info.getVersion() != null) {
                versions.add(info.getVersion().getVersion());
            }
        }
        assertTrue(versions.contains("1"), "Migration V1 phải tồn tại trong flyway_schema_history");
        assertTrue(versions.contains("2"), "Migration V2 phải tồn tại trong flyway_schema_history");
        assertTrue(versions.contains("3"), "Migration V3 phải tồn tại trong flyway_schema_history");
    }

    @Test
    @DisplayName("Toàn bộ Database Index & Unique Index tồn tại trong lược đồ DB")
    void testRequiredIndexesExistInDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Set<String> productIndexes = getTableIndexes(conn, "products");
            assertTrue(productIndexes.contains("idx_products_category_price"), "Thiếu index Products(category_id, price)");
            assertTrue(productIndexes.contains("idx_products_deleted_category_price"));
            assertTrue(productIndexes.contains("idx_products_deleted_price"));
            assertTrue(productIndexes.contains("idx_products_name"));

            Set<String> orderIndexes = getTableIndexes(conn, "orders");
            assertTrue(orderIndexes.contains("idx_orders_user_date_status"), "Thiếu index Orders(user_id, order_date, status)");
            assertTrue(orderIndexes.contains("idx_orders_order_date"));
            assertTrue(orderIndexes.contains("idx_orders_status"));
            assertTrue(orderIndexes.contains("uk_orders_idempotency_key"));
            assertTrue(orderIndexes.contains("uk_orders_payment_tx_id"));
            assertTrue(orderIndexes.contains("idx_orders_status_expires_at"));

            Set<String> outboxIndexes = getTableIndexes(conn, "outbox_events");
            assertTrue(outboxIndexes.contains("idx_outbox_status_next_retry"), "Thiếu index outbox_events(status, next_retry_at)");

            Set<String> userIndexes = getTableIndexes(conn, "users");
            assertTrue(userIndexes.contains("uk_users_email"), "Thiếu unique index Users(email)");
            assertTrue(userIndexes.contains("idx_users_phone"));

            Set<String> couponIndexes = getTableIndexes(conn, "coupons");
            assertTrue(couponIndexes.contains("uk_coupons_code"), "Thiếu unique index Coupons(code)");

            Set<String> orderItemIndexes = getTableIndexes(conn, "order_items");
            assertTrue(orderItemIndexes.contains("idx_order_items_order_id"));
            assertTrue(orderItemIndexes.contains("idx_order_items_product_id"));
        }
    }

    @Test
    @DisplayName("Unique Index Users(email) chặn trùng lặp email ở tầng Database")
    void testUniqueIndex_UsersEmail_RejectsDuplicate() {
        String dupEmail = "unique_check_" + System.currentTimeMillis() + "@test.com";

        User u1 = new User();
        u1.setEmail(dupEmail);
        u1.setUsername("User 1");
        u1.setPassword("hash1");
        u1.setRole("ROLE_CLIENT");
        userRepository.saveAndFlush(u1);

        User u2 = new User();
        u2.setEmail(dupEmail);
        u2.setUsername("User 2");
        u2.setPassword("hash2");
        u2.setRole("ROLE_CLIENT");

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(u2));
    }

    @Test
    @DisplayName("Unique Index Coupons(code) chặn trùng lặp mã giảm giá ở tầng Database")
    void testUniqueIndex_CouponsCode_RejectsDuplicate() {
        String dupCode = "UNIQUE_CODE_" + System.currentTimeMillis();

        Coupon c1 = new Coupon();
        c1.setCode(dupCode);
        c1.setDiscountAmount(new BigDecimal("10000.00"));
        c1.setQuantity(5);
        c1.setActive(true);
        couponRepository.saveAndFlush(c1);

        Coupon c2 = new Coupon();
        c2.setCode(dupCode);
        c2.setDiscountAmount(new BigDecimal("20000.00"));
        c2.setQuantity(10);
        c2.setActive(true);

        assertThrows(DataIntegrityViolationException.class, () -> couponRepository.saveAndFlush(c2));
    }
}
