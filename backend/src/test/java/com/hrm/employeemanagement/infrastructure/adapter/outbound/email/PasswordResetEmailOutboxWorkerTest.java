package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.port.outbound.email.EmailSenderPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.PasswordResetEmailOutboxJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email.SpringDataPasswordResetEmailOutboxRepository;

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

        // 1. Xác minh email port được gọi với token ban đầu ngoài transaction
        verify(emailPort, times(1)).sendPasswordResetEmail(
                eq("user@example.com"),
                eq("testuser"),
                eq("secret-raw-reset-token-12345"),
                eq(30L)
        );

        // 2. Xác minh cột reset_token đã bị xóa rỗng và deliveredAt đã được ghi nhận
        assertThat(outboxItem.getResetToken()).isEmpty();
        assertThat(outboxItem.getDeliveredAt()).isNotNull();
        assertThat(outboxItem.getLastError()).isNull();
    }

    @Test
    @DisplayName("Gửi email thất bại: Worker lên lịch retry, tăng số lần thử và không làm gián đoạn scheduler")
    void deliverNext_whenEmailSendingFails_shouldScheduleRetry() {
        PasswordResetEmailOutboxJpaEntity outboxItem = new PasswordResetEmailOutboxJpaEntity(
                "user.fail@example.com",
                "failuser",
                "fail-token-123",
                30
        );

        when(repository.findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(any(Instant.class)))
                .thenReturn(Optional.of(outboxItem));

        doThrow(new RuntimeException("Gmail network timeout"))
                .when(emailPort).sendPasswordResetEmail(any(), any(), any(), anyLong());

        worker.deliverNext();

        // 1. Xác nhận email port đã được gọi
        verify(emailPort, times(1)).sendPasswordResetEmail(any(), any(), any(), anyLong());

        // 2. Không đánh dấu delivered, mà lên lịch retry
        assertThat(outboxItem.getDeliveredAt()).isNull();
        assertThat(outboxItem.getAttempts()).isEqualTo(1);
        assertThat(outboxItem.getLastError()).contains("Gmail network timeout");
        assertThat(outboxItem.getAvailableAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Token đã hết hạn trước khi gửi: không gửi email, xóa token rỗng và đánh dấu expired")
    void deliverNext_whenExpired_shouldNotSendEmailAndClearToken() {
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

        // 2. Token vẫn bị xóa rỗng và đánh dấu EXPIRED
        assertThat(expiredItem.getResetToken()).isEmpty();
        assertThat(expiredItem.getDeliveredAt()).isNotNull();
        assertThat(expiredItem.getLastError()).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("Khi outbox rỗng: Worker không thực hiện thao tác nào")
    void deliverNext_whenQueueEmpty_shouldDoNothing() {
        when(repository.findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(any(Instant.class)))
                .thenReturn(Optional.empty());

        worker.deliverNext();

        verify(emailPort, never()).sendPasswordResetEmail(any(), any(), any(), anyLong());
    }
}
