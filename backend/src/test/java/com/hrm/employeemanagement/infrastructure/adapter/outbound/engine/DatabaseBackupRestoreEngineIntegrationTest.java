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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    @DisplayName("FULL_BACKUP_TABLES phải bao gồm các bảng cốt lõi nhưng loại bỏ các bảng nhạy cảm (token / outbox)")
    void fullBackup_shouldContainRequiredTablesAndExcludeSensitiveTables() {
        // 1. Phải chứa các bảng nghiệp vụ cốt lõi
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

        // 2. Phải LOẠI BỎ các bảng nhạy cảm chứa token và hàng đợi gửi mail
        assertThat(DatabaseBackupRestoreEngineAdapter.FULL_BACKUP_TABLES)
                .doesNotContain(
                        "password_reset_tokens",
                        "password_reset_email_outbox",
                        "notification_email_outbox",
                        "notification_email_digest_items"
                );
    }

    @Test
    @DisplayName("Thực hiện quy trình Sao lưu FULL (AES-GCM encrypted) -> Phục hồi FULL thành công")
    void testFullBackupAndRestoreFlow(@TempDir Path tempDir) throws Exception {
        File backupFile = tempDir.resolve("full_backup_test.json").toFile();

        // 1. Thực hiện Sao lưu FULL
        File result = engineAdapter.performBackup(backupFile, BackupType.FULL);
        assertThat(result).exists();
        assertThat(result.length()).isGreaterThan(0);

        // 2. Xác minh file trên đĩa đã được mã hóa AES-GCM, không chứa plaintext JSON hay token nhạy cảm
        byte[] fileBytes = Files.readAllBytes(backupFile.toPath());
        String rawContent = new String(fileBytes, StandardCharsets.UTF_8);

        assertThat(rawContent).startsWith("ENC_BACKUP_GCM_V1:");
        assertThat(rawContent).doesNotContain("password_reset_tokens");
        assertThat(rawContent).doesNotContain("password_reset_email_outbox");
        assertThat(rawContent).doesNotContain("password_hash");

        // 3. Kiểm tra phục hồi từ file sao lưu đã mã hóa
        engineAdapter.performRestore(backupFile, BackupType.FULL);

        // 4. Xác minh tính toàn vẹn của dữ liệu sau khi phục hồi
        Integer roleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        assertThat(roleCount).isNotNull().isGreaterThan(0);
    }
}
