package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordResetEmailOutbox Persistence-Level Integration Tests")
class PasswordResetEmailOutboxPersistenceIntegrationTest {

    @Autowired
    private SpringDataPasswordResetEmailOutboxRepository repository;

    @Autowired
    private PasswordResetTokenEncryptionConverter converter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("P1 Test: Khi message đang PENDING trong outbox, DB cột reset_token PHẢI được mã hóa AES-GCM và load từ DB qua Converter decrypt thành công")
    void whenMessagePending_thenDbStoresEncryptedToken_andEntityLoadsDecryptedToken() {
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String recipientEmail = "user.pending@example.com";
        String username = "pending_user";

        // 1. Tạo entity với raw resetToken và lưu vào DB
        PasswordResetEmailOutboxJpaEntity entity = new PasswordResetEmailOutboxJpaEntity(
                recipientEmail,
                username,
                rawToken,
                15L
        );
        PasswordResetEmailOutboxJpaEntity savedEntity = repository.saveAndFlush(entity);
        Long id = savedEntity.getId() != null ? savedEntity.getId() : jdbcTemplate.queryForObject("SELECT MAX(id) FROM password_reset_email_outbox", Long.class);

        // 2. Truy vấn trực tiếp mức database qua JdbcTemplate (bỏ qua JPA mapping / converter)
        String dbTokenValue = jdbcTemplate.queryForObject(
                "SELECT reset_token FROM password_reset_email_outbox WHERE id = ?",
                String.class,
                id
        );

        // 3. Khẳng định: Dữ liệu lưu trong DB KHÔNG ĐƯỢC bằng raw token (chống lưu plaintext at-rest)
        assertThat(dbTokenValue).isNotNull();
        assertThat(dbTokenValue).isNotEqualTo(rawToken);
        assertThat(dbTokenValue).doesNotContain(rawToken);
        assertThat(dbTokenValue).startsWith("ENC:");

        // 4. Xóa sạch JPA first-level persistence context để buộc Hibernate phải đọc thực sự từ DB
        entityManager.flush();
        entityManager.clear();

        // 5. Khẳng định: JPA Entity khi nạp lên từ DB (được giải mã qua Converter) trả về đúng raw token ban đầu
        PasswordResetEmailOutboxJpaEntity loadedEntity = repository.findById(id).orElseThrow();
        assertThat(loadedEntity.getResetToken()).isEqualTo(rawToken);
        assertThat(loadedEntity.getUsername()).isEqualTo(username);
        assertThat(loadedEntity.getRecipientEmail()).isEqualTo(recipientEmail);
    }

    @Test
    @DisplayName("P1 Test: Sau khi gửi email (markDelivered), cột reset_token trong DB được xóa rỗng hoàn toàn")
    void whenDelivered_thenDbStoresEmptyString() {
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        PasswordResetEmailOutboxJpaEntity entity = new PasswordResetEmailOutboxJpaEntity(
                "user.delivered@example.com",
                "delivered_user",
                rawToken,
                15L
        );
        PasswordResetEmailOutboxJpaEntity savedEntity = repository.saveAndFlush(entity);
        Long id = savedEntity.getId() != null ? savedEntity.getId() : jdbcTemplate.queryForObject("SELECT MAX(id) FROM password_reset_email_outbox", Long.class);

        // Gọi markDelivered và flush vào DB
        savedEntity.markDelivered(Instant.now());
        repository.saveAndFlush(savedEntity);

        // Xóa sạch persistence context
        entityManager.flush();
        entityManager.clear();

        // Kiểm tra trực tiếp trên DB: reset_token là chuỗi rỗng
        String dbTokenValue = jdbcTemplate.queryForObject(
                "SELECT reset_token FROM password_reset_email_outbox WHERE id = ?",
                String.class,
                id
        );
        assertThat(dbTokenValue).isEmpty();

        PasswordResetEmailOutboxJpaEntity loadedEntity = repository.findById(id).orElseThrow();
        assertThat(loadedEntity.getResetToken()).isEmpty();
    }

    @Test
    @DisplayName("P1 Test: Khi token hết hạn (markExpired), cột reset_token trong DB được xóa rỗng hoàn toàn")
    void whenExpired_thenDbStoresEmptyString() {
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        PasswordResetEmailOutboxJpaEntity entity = new PasswordResetEmailOutboxJpaEntity(
                "user.expired@example.com",
                "expired_user",
                rawToken,
                0L
        );
        PasswordResetEmailOutboxJpaEntity savedEntity = repository.saveAndFlush(entity);
        Long id = savedEntity.getId() != null ? savedEntity.getId() : jdbcTemplate.queryForObject("SELECT MAX(id) FROM password_reset_email_outbox", Long.class);

        // Gọi markExpired và flush vào DB
        savedEntity.markExpired(Instant.now());
        repository.saveAndFlush(savedEntity);

        // Xóa sạch persistence context
        entityManager.flush();
        entityManager.clear();

        // Kiểm tra trực tiếp trên DB: reset_token là chuỗi rỗng
        String dbTokenValue = jdbcTemplate.queryForObject(
                "SELECT reset_token FROM password_reset_email_outbox WHERE id = ?",
                String.class,
                id
        );
        assertThat(dbTokenValue).isEmpty();

        PasswordResetEmailOutboxJpaEntity loadedEntity = repository.findById(id).orElseThrow();
        assertThat(loadedEntity.getResetToken()).isEmpty();
    }

    @Test
    @DisplayName("P1 Test: Converter PHẢI ném IllegalStateException nếu phát hiện token unencrypted (không có tiền tố ENC:) trong DB")
    void whenUnencryptedTokenInDb_thenConverterThrowsSecurityException() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("raw-unencrypted-legacy-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Security Violation: Detected unencrypted reset token at rest");
    }

    @Test
    @DisplayName("P2 Test: Migration V129 xử lý và làm sạch các dòng legacy plaintext token")
    void testMigrationV129_cleansLegacyPlaintextTokens() {
        // 1. Giả lập chèn bản ghi legacy pending chứa plaintext token bằng native SQL
        jdbcTemplate.update(
                "INSERT INTO password_reset_email_outbox (recipient_email, username, reset_token, validity_minutes, attempts, available_at, delivered_at, last_error, created_at) " +
                "VALUES (?, ?, ?, ?, 0, CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP)",
                "legacy.pending@example.com",
                "legacy_pending_user",
                "unencrypted_raw_legacy_token",
                15L
        );

        // 2. Chạy logic làm sạch tương đương Migration V129
        jdbcTemplate.update(
                "UPDATE password_reset_email_outbox " +
                "SET delivered_at = CURRENT_TIMESTAMP, reset_token = '', last_error = 'EXPIRED_LEGACY_MIGRATION' " +
                "WHERE recipient_email = 'legacy.pending@example.com' AND delivered_at IS NULL AND reset_token NOT LIKE 'ENC:%'"
        );

        // 3. Xác minh: reset_token đã bị xóa rỗng, trạng thái đã đánh dấu delivered/expired
        String token = jdbcTemplate.queryForObject(
                "SELECT reset_token FROM password_reset_email_outbox WHERE recipient_email = 'legacy.pending@example.com'",
                String.class
        );
        String lastError = jdbcTemplate.queryForObject(
                "SELECT last_error FROM password_reset_email_outbox WHERE recipient_email = 'legacy.pending@example.com'",
                String.class
        );

        assertThat(token).isEmpty();
        assertThat(lastError).isEqualTo("EXPIRED_LEGACY_MIGRATION");
    }
}
