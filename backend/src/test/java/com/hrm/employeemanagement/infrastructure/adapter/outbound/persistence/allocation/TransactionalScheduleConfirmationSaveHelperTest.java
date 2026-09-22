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

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataScheduleConfirmationRepository;

class TransactionalScheduleConfirmationSaveHelperTest {

    private SpringDataScheduleConfirmationRepository repository;
    private TransactionalScheduleConfirmationSaveHelper helper;

    private final Long userId = 100L;
    private final LocalDate monday = LocalDate.of(2026, 9, 21);

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataScheduleConfirmationRepository.class);
        helper = new TransactionalScheduleConfirmationSaveHelper(repository);
    }

    @Test
    @DisplayName("saveConfirmationInIsolatedTransaction - Tạo mới khi chưa có bản ghi")
    void saveConfirmation_NewRecord() {
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ScheduleConfirmationJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime now = LocalDateTime.now();
        var result = helper.saveConfirmationInIsolatedTransaction(userId, monday, now, "127.0.0.1");

        assertThat(result.isNew()).isTrue();
        assertThat(result.entity().getConfirmedAt()).isEqualTo(now);
        assertThat(result.entity().getConfirmationStatus()).isEqualTo("CONFIRMED");
        verify(repository).saveAndFlush(any(ScheduleConfirmationJpaEntity.class));
    }

    @Test
    @DisplayName("saveConfirmationInIsolatedTransaction - Cập nhật khi đã có bản ghi")
    void saveConfirmation_ExistingRecord() {
        LocalDateTime oldTime = LocalDateTime.now().minusDays(1);
        ScheduleConfirmationJpaEntity existing = new ScheduleConfirmationJpaEntity(1L, userId, monday, oldTime, "127.0.0.1");
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(ScheduleConfirmationJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime newTime = LocalDateTime.now();
        var result = helper.saveConfirmationInIsolatedTransaction(userId, monday, newTime, "127.0.0.1");

        assertThat(result.isNew()).isFalse();
        assertThat(result.entity().getConfirmedAt()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("saveConfirmationInIsolatedTransaction - Timestamp safeguard không ghi đè lùi timestamp cũ hơn")
    void saveConfirmation_OlderTimestamp_DoesNotRegress() {
        LocalDateTime latestTime = LocalDateTime.now();
        ScheduleConfirmationJpaEntity existing = new ScheduleConfirmationJpaEntity(1L, userId, monday, latestTime, "127.0.0.1");
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.of(existing));

        LocalDateTime olderTime = latestTime.minusHours(2);
        var result = helper.saveConfirmationInIsolatedTransaction(userId, monday, olderTime, "127.0.0.1");

        assertThat(result.isNew()).isFalse();
        assertThat(result.entity().getConfirmedAt()).isEqualTo(latestTime);
    }

    @Test
    @DisplayName("saveFeedbackInIsolatedTransaction - Tạo mới giữ nguyên confirmedAt = NULL")
    void saveFeedback_NewRecord_KeepsConfirmedAtNull() {
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(ScheduleConfirmationJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime feedbackTime = LocalDateTime.now();
        var result = helper.saveFeedbackInIsolatedTransaction(userId, monday, "Lý do A", feedbackTime, "127.0.0.1");

        assertThat(result.isNew()).isTrue();
        assertThat(result.entity().getConfirmedAt()).isNull();
        assertThat(result.entity().getFeedbackNote()).isEqualTo("Lý do A");
        assertThat(result.entity().getFeedbackAt()).isEqualTo(feedbackTime);
        assertThat(result.entity().getConfirmationStatus()).isEqualTo("HAS_FEEDBACK");
    }

    @Test
    @DisplayName("saveFeedbackInIsolatedTransaction - Cập nhật trên bản ghi đã có -> bảo lưu confirmedAt")
    void saveFeedback_ExistingRecord_PreservesConfirmedAt() {
        LocalDateTime previousConfirmedAt = LocalDateTime.now().minusDays(2);
        ScheduleConfirmationJpaEntity existing = new ScheduleConfirmationJpaEntity(1L, userId, monday, previousConfirmedAt, "127.0.0.1");
        when(repository.findByUserIdAndWeekStartDate(userId, monday)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(ScheduleConfirmationJpaEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime feedbackTime = LocalDateTime.now();
        var result = helper.saveFeedbackInIsolatedTransaction(userId, monday, "Ý kiến B", feedbackTime, "127.0.0.1");

        assertThat(result.isNew()).isFalse();
        assertThat(result.entity().getConfirmedAt()).isEqualTo(previousConfirmedAt);
        assertThat(result.entity().getFeedbackNote()).isEqualTo("Ý kiến B");
        assertThat(result.entity().getFeedbackAt()).isEqualTo(feedbackTime);
        assertThat(result.entity().getConfirmationStatus()).isEqualTo("HAS_FEEDBACK");
    }
}
