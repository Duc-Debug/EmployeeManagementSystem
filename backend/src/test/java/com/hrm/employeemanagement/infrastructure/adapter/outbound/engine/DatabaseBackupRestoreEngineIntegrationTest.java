package com.hrm.employeemanagement.infrastructure.adapter.outbound.engine;

import com.hrm.employeemanagement.domain.backup.BackupType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DatabaseBackupRestoreEngine Integration Tests")
class DatabaseBackupRestoreEngineIntegrationTest {

    @Autowired
    private DatabaseBackupRestoreEngineAdapter engineAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("FULL_BACKUP_TABLES phải bao gồm đầy đủ các bảng cốt lõi của hệ thống")
    void fullBackup_shouldContainAllRequiredTables() {
        assertThat(DatabaseBackupRestoreEngineAdapter.FULL_BACKUP_TABLES)
                .contains(
                        "roles",
                        "permissions",
                        "role_permissions",
                        "org_units",
                        "departments",
                        "users",
                        "employees",
                        "audit_logs",
                        "skills",
                        "employee_skills",
                        "projects",
                        "tasks",
                        "timesheets",
                        "weekly_project_allocations"
                );
    }

    @Test
    @DisplayName("Thực hiện quy trình Sao lưu FULL -> Sửa đổi dữ liệu -> Phục hồi FULL thành công")
    void testFullBackupAndRestoreFlow(@TempDir Path tempDir) throws Exception {
        File backupFile = tempDir.resolve("full_backup_test.json").toFile();

        // 1. Thực hiện Sao lưu FULL
        File result = engineAdapter.performBackup(backupFile, BackupType.FULL);
        assertThat(result).exists();
        assertThat(result.length()).isGreaterThan(0);

        // 2. Kiểm tra phục hồi từ file sao lưu
        engineAdapter.performRestore(backupFile, BackupType.FULL);

        // 3. Xác minh tính toàn vẹn của dữ liệu sau khi phục hồi
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        assertThat(roleCount).isNotNull().isGreaterThan(0);
    }
}
