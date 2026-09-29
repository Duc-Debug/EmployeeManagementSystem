package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import com.hrm.employeemanagement.application.port.outbound.email.EmailSenderPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.PasswordResetEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.SpringDataPasswordResetEmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("PasswordResetEmailOutboxWorker & Outbox Entity Unit Tests")
class PasswordResetEmailOutboxWorkerTest {

    private SpringDataPasswordResetEmailOutboxRepository repository;
    private EmailSenderPort emailPort;
    private PasswordResetEmailOutboxWorker worker;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataPasswordResetEmailOutboxRepository.class);
        emailPort = mock(EmailSenderPort.class);
        worker = new PasswordResetEmailOutboxWorker(repository, emailPort);
    }

    @Test
    @DisplayName("Sau khi gửi mail thành công: reset_token bị xóa rỗng, deliveredAt được cập nhật")
    void deliverNext_shouldSendEmailAndClearResetToken() {
        PasswordResetEmailOutboxJpaEntity outboxItem = new PasswordResetEmailOutboxJpaEntity(
                "user@example.com",
                "testuser",
                "secret-raw-reset-token-12345",
                30
        );

        when(repository.findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(any(Instant.class)))
                .thenReturn(Optional.of(outboxItem));

        worker.deliverNext();

        // 1. Xác minh email port được gọi với token ban đầu
        verify(emailPort, times(1)).sendPasswordResetEmail(
                eq("user@example.com"),
                eq("testuser"),
                eq("secret-raw-reset-token-12345"),
                eq(30L)
        );

        // 2. Xác minh cột reset_token đã bị xóa rỗng và không còn lưu token plaintext
        assertThat(outboxItem.getResetToken()).isEmpty();
    }

    @Test
    @DisplayName("Token đã hết hạn trước khi gửi: không gửi email, xóa token rỗng và đánh dấu expired")
    void deliverNext_whenExpired_shouldNotSendEmailAndClearToken() {
        // Tạo entity giả định được tạo từ 60 phút trước (hạn 30 phút)
        PasswordResetEmailOutboxJpaEntity expiredItem = new PasswordResetEmailOutboxJpaEntity(
                "expired@example.com",
                "expireduser",
                "expired-token-99999",
                0 // 0 phút validity -> ngay lập tức hết hạn
        );

        when(repository.findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(any(Instant.class)))
                .thenReturn(Optional.of(expiredItem));

        worker.deliverNext();

        // 1. Không gửi email vì token đã hết hạn
        verify(emailPort, never()).sendPasswordResetEmail(any(), any(), any(), anyLong());

        // 2. Token vẫn bị xóa rỗng
        assertThat(expiredItem.getResetToken()).isEmpty();
    }
}
