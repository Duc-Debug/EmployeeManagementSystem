package com.hrm.employeemanagement.infrastructure.adapter.outbound.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service hỗ trợ Distributed Lock dựa trên cơ chế Advisory Lock của MySQL (GET_LOCK / RELEASE_LOCK).
 * Đảm bảo các tác vụ định kỳ (như Retention Purge) chỉ chạy trên duy nhất 1 replica trong môi trường multi-instance.
 * Nếu chạy với database không phải MySQL (như H2 trong test), tác vụ sẽ được thực thi trực tiếp an toàn.
 */
@Component
public class DbDistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DbDistributedLockService.class);

    private final DataSource dataSource;

    public DbDistributedLockService(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    public boolean executeWithLock(String lockName, int timeoutSeconds, Runnable task) {
        Objects.requireNonNull(lockName, "lockName must not be null");
        Objects.requireNonNull(task, "task must not be null");

        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            boolean isMySql = dbProduct != null && dbProduct.toLowerCase().contains("mysql");

            if (!isMySql) {
                // Môi trường test / H2 -> Chạy trực tiếp
                task.run();
                return true;
            }

            // MySQL: sử dụng GET_LOCK(str, timeout)
            try (PreparedStatement stmt = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
                stmt.setString(1, lockName);
                stmt.setInt(2, timeoutSeconds);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        try {
                            log.info("Đã nhận distributed lock '{}', bắt đầu thực thi tác vụ.", lockName);
                            task.run();
                            return true;
                        } finally {
                            try (PreparedStatement releaseStmt = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                                releaseStmt.setString(1, lockName);
                                releaseStmt.executeQuery();
                                log.info("Đã giải phóng distributed lock '{}'.", lockName);
                            } catch (SQLException e) {
                                log.warn("Lỗi khi giải phóng lock '{}': {}", lockName, e.getMessage());
                            }
                        }
                    } else {
                        log.info("Không thể nhận distributed lock '{}' (đang được giữ bởi replica khác), bỏ qua lần chạy này.", lockName);
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi kiểm tra distributed lock '{}': {}", lockName, e.getMessage(), e);
            return false;
        }
    }
}
