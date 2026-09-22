package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.ScheduleConfirmationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataScheduleConfirmationRepository;

/**
 * Helper thực hiện lưu ScheduleConfirmation trong một Transaction cô lập (REQUIRES_NEW).
 * Mục đích:
 * 1. Find, mutate và saveAndFlush diễn ra hoàn toàn bên trong transaction cô lập,
 *    tránh rò rỉ managed entity vào outer transaction gây OptimisticLockException.
 * 2. Khi xảy ra xung đột DuplicateKey (race condition concurrent insert),
 *    chỉ inner transaction bị rollback, outer transaction vẫn sạch và không bị đánh dấu 'rollback-only'.
 */
@Component
public class TransactionalScheduleConfirmationSaveHelper {

    private final SpringDataScheduleConfirmationRepository repository;

    public TransactionalScheduleConfirmationSaveHelper(SpringDataScheduleConfirmationRepository repository) {
        this.repository = repository;
    }

    public record IsolatedSaveResult(ScheduleConfirmationJpaEntity entity, boolean isNew) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IsolatedSaveResult saveConfirmationInIsolatedTransaction(
            Long userId,
            LocalDate weekStartDate,
            LocalDateTime confirmedAt,
            String ipAddress) {

        Optional<ScheduleConfirmationJpaEntity> opt = repository.findByUserIdAndWeekStartDate(userId, weekStartDate);
        boolean isNew = opt.isEmpty();
        ScheduleConfirmationJpaEntity entity = opt.orElseGet(ScheduleConfirmationJpaEntity::new);

        if (isNew) {
            entity.setUserId(userId);
            entity.setWeekStartDate(weekStartDate);
            entity.setConfirmedAt(confirmedAt);
            entity.setConfirmationStatus("CONFIRMED");
            entity.setIpAddress(ipAddress);
        } else {
            // Safeguard: Never overwrite with an older confirmation timestamp (atomic greatest)
            if (entity.getConfirmedAt() != null && confirmedAt.isBefore(entity.getConfirmedAt())) {
                return new IsolatedSaveResult(entity, false);
            }
            entity.setConfirmedAt(confirmedAt);
            entity.setConfirmationStatus("CONFIRMED");
            entity.setIpAddress(ipAddress);
        }

        ScheduleConfirmationJpaEntity saved = repository.saveAndFlush(entity);
        return new IsolatedSaveResult(saved, isNew);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IsolatedSaveResult saveFeedbackInIsolatedTransaction(
            Long userId,
            LocalDate weekStartDate,
            String feedbackNote,
            LocalDateTime feedbackAt,
            String ipAddress) {

        Optional<ScheduleConfirmationJpaEntity> opt = repository.findByUserIdAndWeekStartDate(userId, weekStartDate);
        boolean isNew = opt.isEmpty();
        ScheduleConfirmationJpaEntity entity = opt.orElseGet(ScheduleConfirmationJpaEntity::new);

        if (isNew) {
            entity.setUserId(userId);
            entity.setWeekStartDate(weekStartDate);
            // confirmedAt remains null for feedback-only record
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

        ScheduleConfirmationJpaEntity saved = repository.saveAndFlush(entity);
        return new IsolatedSaveResult(saved, isNew);
    }
}