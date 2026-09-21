package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataScheduleConfirmationRepository;

@Component
public class ScheduleConfirmationJpaAdapter implements ScheduleConfirmationPort {

    private static final Logger log = LoggerFactory.getLogger(ScheduleConfirmationJpaAdapter.class);

    private final SpringDataScheduleConfirmationRepository repository;
    private final TransactionalScheduleConfirmationSaveHelper saveHelper;

    public ScheduleConfirmationJpaAdapter(
            SpringDataScheduleConfirmationRepository repository,
            TransactionalScheduleConfirmationSaveHelper saveHelper) {
        this.repository = repository;
        this.saveHelper = saveHelper;
    }

    @Override
    public Optional<ScheduleConfirmationRecord> findByUserIdAndWeek(Long userId, LocalDate weekStartDate) {
        return repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                .map(this::toRecord);
    }

    @Override
    public List<ScheduleConfirmationRecord> findByUserIdAndWeeks(Long userId, List<LocalDate> weekStartDates) {
        if (weekStartDates == null || weekStartDates.isEmpty()) {
            return List.of();
        }
        return repository.findByUserIdAndWeekStartDateIn(userId, weekStartDates).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    public SaveConfirmationResult saveConfirmation(Long userId, LocalDate weekStartDate, LocalDateTime confirmedAt, String ipAddress) {
        boolean isNew = false;
        ScheduleConfirmationJpaEntity entity = repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                .orElse(null);

        if (entity == null) {
            isNew = true;
            entity = new ScheduleConfirmationJpaEntity();
            entity.setUserId(userId);
            entity.setWeekStartDate(weekStartDate);
            entity.setConfirmedAt(confirmedAt);
            entity.setConfirmationStatus("CONFIRMED");
            entity.setIpAddress(ipAddress);
        } else {
            // Safeguard: Never overwrite with an older confirmation timestamp (atomic greatest)
            if (entity.getConfirmedAt() != null && confirmedAt.isBefore(entity.getConfirmedAt())) {
                return new SaveConfirmationResult(toRecord(entity), false);
            }
            entity.setConfirmedAt(confirmedAt);
            entity.setConfirmationStatus("CONFIRMED");
            entity.setIpAddress(ipAddress);
        }

        try {
            ScheduleConfirmationJpaEntity saved = saveHelper.saveInIsolatedTransaction(entity);
            return new SaveConfirmationResult(toRecord(saved), isNew);
        } catch (DataIntegrityViolationException | ConcurrencyFailureException ex) {
            log.warn("Race condition hoặc optimistic lock conflict khi lưu schedule confirmation cho user {} tuần {}. Đã cô lập transaction và reload bản ghi đã commit thành công.",
                    userId, weekStartDate);
            ScheduleConfirmationRecord reloaded = repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                    .map(this::toRecord)
                    .orElseThrow(() -> ex);
            return new SaveConfirmationResult(reloaded, false);
        }
    }

    @Override
    public SaveConfirmationResult saveFeedback(Long userId, LocalDate weekStartDate, String feedbackNote, LocalDateTime feedbackAt, String ipAddress) {
        boolean isNew = false;
        ScheduleConfirmationJpaEntity entity = repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                .orElse(null);

        if (entity == null) {
            isNew = true;
            entity = new ScheduleConfirmationJpaEntity();
            entity.setUserId(userId);
            entity.setWeekStartDate(weekStartDate);
            entity.setConfirmedAt(feedbackAt);
            entity.setFeedbackNote(feedbackNote);
            entity.setFeedbackAt(feedbackAt);
            entity.setConfirmationStatus("HAS_FEEDBACK");
            entity.setIpAddress(ipAddress);
        } else {
            entity.setFeedbackNote(feedbackNote);
            entity.setFeedbackAt(feedbackAt);
            entity.setConfirmationStatus("HAS_FEEDBACK");
            entity.setIpAddress(ipAddress);
        }

        try {
            ScheduleConfirmationJpaEntity saved = saveHelper.saveInIsolatedTransaction(entity);
            return new SaveConfirmationResult(toRecord(saved), isNew);
        } catch (DataIntegrityViolationException | ConcurrencyFailureException ex) {
            log.warn("Race condition hoặc optimistic lock conflict khi lưu schedule feedback cho user {} tuần {}. Đã cô lập transaction và reload bản ghi đã commit thành công.",
                    userId, weekStartDate);
            ScheduleConfirmationRecord reloaded = repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                    .map(this::toRecord)
                    .orElseThrow(() -> ex);
            return new SaveConfirmationResult(reloaded, false);
        }
    }

    private ScheduleConfirmationRecord toRecord(ScheduleConfirmationJpaEntity entity) {
        return new ScheduleConfirmationRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getWeekStartDate(),
                entity.getConfirmedAt(),
                entity.getIpAddress(),
                entity.getFeedbackNote(),
                entity.getFeedbackAt(),
                entity.getConfirmationStatus()
        );
    }
}
