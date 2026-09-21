package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.empty());

        LocalDateTime now = LocalDateTime.now();
        ScheduleConfirmationJpaEntity savedEntity = new ScheduleConfirmationJpaEntity(1L, userId, monday, now, "127.0.0.1");
        when(saveHelper.saveInIsolatedTransaction(any(ScheduleConfirmationJpaEntity.class))).thenReturn(savedEntity);

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, now, "127.0.0.1");

        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.record().confirmedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Cập nhật bản ghi hiện có khi re-confirm -> newlyCreated = false")
    void saveConfirmation_ExistingRecord_Success() {
        LocalDateTime oldTime = LocalDateTime.now().minusDays(1);
        ScheduleConfirmationJpaEntity existing = new ScheduleConfirmationJpaEntity(1L, userId, monday, oldTime, "127.0.0.1");
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.of(existing));

        LocalDateTime newTime = LocalDateTime.now();
        ScheduleConfirmationJpaEntity updated = new ScheduleConfirmationJpaEntity(1L, userId, monday, newTime, "127.0.0.1");
        when(saveHelper.saveInIsolatedTransaction(any(ScheduleConfirmationJpaEntity.class))).thenReturn(updated);

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, newTime, "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().confirmedAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("OptimisticLockingFailureException khi concurrent re-confirm -> Reload bản ghi đã commit thành công")
    void saveConfirmation_OptimisticLockingConflict_ReloadsAndReturnsIdempotent() {
        LocalDateTime oldTime = LocalDateTime.now().minusDays(1);
        ScheduleConfirmationJpaEntity existing = new ScheduleConfirmationJpaEntity(1L, userId, monday, oldTime, "127.0.0.1");
        LocalDateTime committedTime = LocalDateTime.now();
        ScheduleConfirmationJpaEntity committedEntity = new ScheduleConfirmationJpaEntity(1L, userId, monday, committedTime, "127.0.0.1");

        when(repository.findByUserIdAndWeekStartDate(userId, monday))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.of(committedEntity));

        when(saveHelper.saveInIsolatedTransaction(any(ScheduleConfirmationJpaEntity.class)))
                .thenThrow(new OptimisticLockingFailureException("Version conflict"));

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, LocalDateTime.now(), "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().confirmedAt()).isEqualTo(committedTime);
    }

    @Test
    @DisplayName("DataIntegrityViolationException khi concurrent insert -> Reload bản ghi đã commit thành công")
    void saveConfirmation_DataIntegrityViolation_ReloadsAndReturnsIdempotent() {
        when(repository.findByUserIdAndWeekStartDate(userId, monday))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ScheduleConfirmationJpaEntity(1L, userId, monday, LocalDateTime.now(), "127.0.0.1")));

        when(saveHelper.saveInIsolatedTransaction(any(ScheduleConfirmationJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, LocalDateTime.now(), "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Timestamp safeguard: Không bao giờ ghi đè lùi timestamp cũ hơn")
    void saveConfirmation_OlderTimestamp_DoesNotRegress() {
        LocalDateTime latestTime = LocalDateTime.now();
        ScheduleConfirmationJpaEntity existing = new ScheduleConfirmationJpaEntity(1L, userId, monday, latestTime, "127.0.0.1");
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.of(existing));

        LocalDateTime olderTime = latestTime.minusHours(2);
        SaveConfirmationResult result = adapter.saveConfirmation(userId, monday, olderTime, "127.0.0.1");

        assertThat(result.newlyCreated()).isFalse();
        assertThat(result.record().confirmedAt()).isEqualTo(latestTime);
    }
}