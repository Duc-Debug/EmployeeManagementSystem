package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public ScheduleConfirmationRecord saveConfirmation(Long userId, LocalDate weekStartDate, LocalDateTime confirmedAt, String ipAddress) {
        ScheduleConfirmationJpaEntity entity = repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                .orElseGet(() -> {
                    ScheduleConfirmationJpaEntity newEntity = new ScheduleConfirmationJpaEntity();
                    newEntity.setUserId(userId);
                    newEntity.setWeekStartDate(weekStartDate);
                    return newEntity;
                });

        entity.setConfirmedAt(confirmedAt);
        entity.setIpAddress(ipAddress);

        try {
            ScheduleConfirmationJpaEntity saved = saveHelper.saveInIsolatedTransaction(entity);
            return toRecord(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition phát hiện khi lưu schedule confirmation cho user {} tuần {}. Đã cô lập transaction và reload bản ghi đã commit thành công.",
                    userId, weekStartDate);
            return repository.findByUserIdAndWeekStartDate(userId, weekStartDate)
                    .map(this::toRecord)
                    .orElseThrow(() -> ex);
        }
    }

    private ScheduleConfirmationRecord toRecord(ScheduleConfirmationJpaEntity entity) {
        return new ScheduleConfirmationRecord(
                entity.getId(),
                entity.getUserId(),
                entity.getWeekStartDate(),
                entity.getConfirmedAt(),
                entity.getIpAddress()
        );
    }
}