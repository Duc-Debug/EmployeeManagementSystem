package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email;

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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordResetEmailOutbox Persistence-Level Integration Tests")
class PasswordResetEmailOutboxPersistenceIntegrationTest {

    @Autowired
    private SpringDataPasswordResetEmailOutboxRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("P1 Test: Khi message đang PENDING trong outbox, DB cột reset_token PHẢI được mã hóa AES-GCM (không chứa plaintext token)")
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

        // 4. Khẳng định: JPA Entity khi nạp lên (được giải mã) vẫn trả về đúng raw token ban đầu cho worker
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
}
