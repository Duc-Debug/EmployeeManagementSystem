package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort.SaveConfirmationResult;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataScheduleConfirmationRepository;

class ScheduleConfirmationJpaAdapterTest {

    private SpringDataScheduleConfirmationRepository repository;
    private TransactionalScheduleConfirmationSaveHelper saveHelper;
    private ScheduleConfirmationJpaAdapter adapter;

    private final Long userId = 100L;
    private final LocalDate monday = LocalDate.of(2026, 9, 21);

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataScheduleConfirmationRepository.class);
        saveHelper = mock(TransactionalScheduleConfirmationSaveHelper.class);
        adapter = new ScheduleConfirmationJpaAdapter(repository, saveHelper);
    }

    @Test
    @DisplayName("Lưu mới thành công -> newlyCreated = true")
    void saveConfirmation_NewRecord_Success() {
        LocalDateTime now = LocalDateTime.now();
        ScheduleConfirmationJpaEntity savedEntity = new ScheduleConfirmationJpaEntity(1L, userId, monday, now, "127.0.0.1");
        when(saveHelper.saveConfirmationInIsolatedTransaction(eq(userId), eq(monday), eq(now), eq("127.0.0.1")))
                .thenReturn(new TransactionalScheduleConfirmationSaveHelper.IsolatedSaveResult(savedEntity, true));

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, now, "127.0.0.1");

        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.record().confirmedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Cập nhật bản ghi hiện có khi re-confirm -> newlyCreated = false")
    void saveConfirmation_ExistingRecord_Success() {
        LocalDateTime newTime = LocalDateTime.now();
        ScheduleConfirmationJpaEntity updated = new ScheduleConfirmationJpaEntity(1L, userId, monday, newTime, "127.0.0.1");
        when(saveHelper.saveConfirmationInIsolatedTransaction(eq(userId), eq(monday), eq(newTime), eq("127.0.0.1")))
                .thenReturn(new TransactionalScheduleConfirmationSaveHelper.IsolatedSaveResult(updated, false));

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, newTime, "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().confirmedAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("OptimisticLockingFailureException khi concurrent re-confirm -> Reload bản ghi đã commit thành công")
    void saveConfirmation_OptimisticLockingConflict_ReloadsAndReturnsIdempotent() {
        LocalDateTime committedTime = LocalDateTime.now();
        ScheduleConfirmationJpaEntity committedEntity = new ScheduleConfirmationJpaEntity(1L, userId, monday, committedTime, "127.0.0.1");

        when(saveHelper.saveConfirmationInIsolatedTransaction(eq(userId), eq(monday), any(), eq("127.0.0.1")))
                .thenThrow(new OptimisticLockingFailureException("Version conflict"));
        when(repository.findByUserIdAndWeekStartDate(userId, monday))
                .thenReturn(Optional.of(committedEntity));

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, LocalDateTime.now(), "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().confirmedAt()).isEqualTo(committedTime);
    }

    @Test
    @DisplayName("DataIntegrityViolationException khi concurrent insert -> Reload bản ghi đã commit thành công")
    void saveConfirmation_DataIntegrityViolation_ReloadsAndReturnsIdempotent() {
        when(saveHelper.saveConfirmationInIsolatedTransaction(eq(userId), eq(monday), any(), eq("127.0.0.1")))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(repository.findByUserIdAndWeekStartDate(userId, monday))
                .thenReturn(Optional.of(new ScheduleConfirmationJpaEntity(1L, userId, monday, LocalDateTime.now(), "127.0.0.1")));

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, LocalDateTime.now(), "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("saveFeedback lần đầu (chưa có confirmation record) -> confirmedAt phải giữ NULL")
    void saveFeedback_NewRecord_KeepsConfirmedAtNull() {
        LocalDateTime feedbackTime = LocalDateTime.now();
        String reason = "Cần điều chỉnh giờ";
        ScheduleConfirmationJpaEntity savedEntity = new ScheduleConfirmationJpaEntity();
        savedEntity.setId(2L);
        savedEntity.setUserId(userId);
        savedEntity.setWeekStartDate(monday);
        savedEntity.setConfirmedAt(null);
        savedEntity.setFeedbackNote(reason);
        savedEntity.setFeedbackAt(feedbackTime);
        savedEntity.setConfirmationStatus("HAS_FEEDBACK");
        savedEntity.setIpAddress("127.0.0.1");

        when(saveHelper.saveFeedbackInIsolatedTransaction(eq(userId), eq(monday), eq(reason), eq(feedbackTime), eq("127.0.0.1")))
                .thenReturn(new TransactionalScheduleConfirmationSaveHelper.IsolatedSaveResult(savedEntity, true));

        SaveConfirmationResult result = adapter.saveFeedback(userId, monday, reason, feedbackTime, "127.0.0.1");

        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.record().confirmedAt()).isNull();
        assertThat(result.record().feedbackNote()).isEqualTo(reason);
        assertThat(result.record().feedbackAt()).isEqualTo(feedbackTime);
        assertThat(result.record().confirmationStatus()).isEqualTo("HAS_FEEDBACK");
    }

    @Test
    @DisplayName("saveFeedback trên bản ghi đã có confirmation -> giữ nguyên confirmedAt trước đó")
    void saveFeedback_ExistingRecord_PreservesConfirmedAt() {
        LocalDateTime previousConfirmedAt = LocalDateTime.now().minusDays(2);
        LocalDateTime feedbackTime = LocalDateTime.now();
        String reason = "Ý kiến phản hồi bổ sung";
        ScheduleConfirmationJpaEntity savedEntity = new ScheduleConfirmationJpaEntity();
        savedEntity.setId(1L);
        savedEntity.setUserId(userId);
        savedEntity.setWeekStartDate(monday);
        savedEntity.setConfirmedAt(previousConfirmedAt);
        savedEntity.setFeedbackNote(reason);
        savedEntity.setFeedbackAt(feedbackTime);
        savedEntity.setConfirmationStatus("HAS_FEEDBACK");
        savedEntity.setIpAddress("127.0.0.1");

        when(saveHelper.saveFeedbackInIsolatedTransaction(eq(userId), eq(monday), eq(reason), eq(feedbackTime), eq("127.0.0.1")))
                .thenReturn(new TransactionalScheduleConfirmationSaveHelper.IsolatedSaveResult(savedEntity, false));

        SaveConfirmationResult result = adapter.saveFeedback(userId, monday, reason, feedbackTime, "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().confirmedAt()).isEqualTo(previousConfirmedAt);
        assertThat(result.record().feedbackNote()).isEqualTo(reason);
        assertThat(result.record().confirmationStatus()).isEqualTo("HAS_FEEDBACK");
    }
}