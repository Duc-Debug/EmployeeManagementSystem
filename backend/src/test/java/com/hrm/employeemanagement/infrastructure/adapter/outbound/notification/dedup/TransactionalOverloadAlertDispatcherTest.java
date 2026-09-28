package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;
import com.hrm.employeemanagement.application.port.inbound.notification.CreateNotificationEventUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.OverloadAlertDispatchResult;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupRecordJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository.SpringDataNotificationDedupRecordRepository;

class TransactionalOverloadAlertDispatcherTest {

    private SpringDataNotificationDedupRecordRepository recordRepository;
    private CreateNotificationEventUseCase createNotificationEventUseCase;
    private TransactionalOverloadAlertDispatcher dispatcher;

    private EmployeeWeeklyOverloadCandidate candidate;
    private LocalDateTime now;
    private LocalDateTime expiresAt;

    @BeforeEach
    void setUp() {
        recordRepository = mock(SpringDataNotificationDedupRecordRepository.class);
        createNotificationEventUseCase = mock(CreateNotificationEventUseCase.class);

        dispatcher = new TransactionalOverloadAlertDispatcher(
                recordRepository,
                createNotificationEventUseCase
        );

        candidate = new EmployeeWeeklyOverloadCandidate(
                101L,
                "NV001",
                "Nguyễn Văn A",
                2026,
                38,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(40),
                true,
                List.of(201L)
        );

        now = LocalDateTime.of(2026, 9, 21, 10, 0);
        expiresAt = now.plusDays(7);
    }

    @Test
    @DisplayName("Khóa dedup active đã tồn tại -> Trả về SKIPPED_DEDUP và không tạo thêm thông báo")
    void dispatch_whenActiveDedupKeyAlreadyExists_returnsSkippedDedup() {
        when(recordRepository.findByActiveDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.of(new NotificationDedupRecordJpaEntity()));

        OverloadAlertDispatchResult result = dispatcher.dispatchOverloadAlert(
                candidate,
                201L,
                "2026-W38",
                now,
                expiresAt
        );

        assertEquals(OverloadAlertDispatchResult.SKIPPED_DEDUP, result);
        verify(recordRepository, never()).saveAndFlush(any());
        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Xung đột đồng thời (Race condition) vi phạm uk_notif_dedup_active_key -> Bắt DataIntegrityViolationException và trả về SKIPPED_DEDUP")
    void dispatch_whenConcurrencyConflictOnSave_returnsSkippedDedup() {
        when(recordRepository.findByActiveDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.empty());
        when(recordRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_notif_dedup_active_key'"));

        OverloadAlertDispatchResult result = dispatcher.dispatchOverloadAlert(
                candidate,
                201L,
                "2026-W38",
                now,
                expiresAt
        );

        assertEquals(OverloadAlertDispatchResult.SKIPPED_DEDUP, result);
        verify(createNotificationEventUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Gửi notification thất bại -> Bắt Exception, rollback transaction và trả về FAILED (không để lại dedup giả mạo)")
    void dispatch_whenNotificationEventThrowsException_returnsFailed() {
        when(recordRepository.findByActiveDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.empty());
        when(recordRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("Notification service timeout"))
                .when(createNotificationEventUseCase).execute(any());

        OverloadAlertDispatchResult result = dispatcher.dispatchOverloadAlert(
                candidate,
                201L,
                "2026-W38",
                now,
                expiresAt
        );

        assertEquals(OverloadAlertDispatchResult.FAILED, result);
        verify(recordRepository).saveAndFlush(any());
        verify(createNotificationEventUseCase).execute(any());
    }

    @Test
    @DisplayName("Gửi notification thành công -> Lưu dedup và trả về ALERTED")
    void dispatch_whenSuccessful_returnsAlerted() {
        when(recordRepository.findByActiveDedupKey("OVERLOAD_WARNING:EMPLOYEE:101:2026-W38:USER:201"))
                .thenReturn(Optional.empty());
        when(recordRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(createNotificationEventUseCase.execute(any())).thenReturn(1L);

        OverloadAlertDispatchResult result = dispatcher.dispatchOverloadAlert(
                candidate,
                201L,
                "2026-W38",
                now,
                expiresAt
        );

        assertEquals(OverloadAlertDispatchResult.ALERTED, result);
        verify(recordRepository).saveAndFlush(any());
        verify(createNotificationEventUseCase).execute(any());
    }
}
