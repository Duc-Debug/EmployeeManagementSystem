package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.idleness;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.idleness.LoadProlongedIdlenessAcknowledgementPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.idleness.SaveProlongedIdlenessAcknowledgementPort;
import com.hrm.employeemanagement.domain.allocation.idleness.ProlongedIdlenessAcknowledgement;

@Component
public class ProlongedIdlenessAcknowledgementPersistenceAdapter implements SaveProlongedIdlenessAcknowledgementPort, LoadProlongedIdlenessAcknowledgementPort {

    private final SpringDataProlongedIdlenessAcknowledgementRepository repository;

    public ProlongedIdlenessAcknowledgementPersistenceAdapter(SpringDataProlongedIdlenessAcknowledgementRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ProlongedIdlenessAcknowledgement save(ProlongedIdlenessAcknowledgement acknowledgement) {
        ProlongedIdlenessAcknowledgementJpaEntity entity = repository
                .findByEmployeeIdAndFromYearAndFromWeekAndDurationWeeks(
                        acknowledgement.getEmployeeId(),
                        acknowledgement.getFromYear(),
                        acknowledgement.getFromWeek(),
                        acknowledgement.getDurationWeeks()
                )
                .orElseGet(ProlongedIdlenessAcknowledgementJpaEntity::new);

        entity.setEmployeeId(acknowledgement.getEmployeeId());
        entity.setFromYear(acknowledgement.getFromYear());
        entity.setFromWeek(acknowledgement.getFromWeek());
        entity.setDurationWeeks(acknowledgement.getDurationWeeks());
        entity.setActionTaken(acknowledgement.getActionTaken());
        entity.setNotes(acknowledgement.getNotes());
        entity.setStatus(acknowledgement.getStatus());
        entity.setAcknowledgedBy(acknowledgement.getAcknowledgedBy());
        entity.setAcknowledgedAt(acknowledgement.getAcknowledgedAt());

        ProlongedIdlenessAcknowledgementJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ProlongedIdlenessAcknowledgement> findByEmployeeAndPeriod(Long employeeId, int fromYear, int fromWeek, int durationWeeks) {
        return repository.findByEmployeeIdAndFromYearAndFromWeekAndDurationWeeks(employeeId, fromYear, fromWeek, durationWeeks)
                .map(this::toDomain);
    }

    @Override
    public List<ProlongedIdlenessAcknowledgement> findByPeriodAndEmployees(int fromYear, int fromWeek, int durationWeeks, List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findByPeriodAndEmployeeIds(fromYear, fromWeek, durationWeeks, employeeIds).stream()
                .map(this::toDomain)
                .toList();
    }

    private ProlongedIdlenessAcknowledgement toDomain(ProlongedIdlenessAcknowledgementJpaEntity entity) {
        return new ProlongedIdlenessAcknowledgement(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getFromYear(),
                entity.getFromWeek(),
                entity.getDurationWeeks(),
                entity.getActionTaken(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getAcknowledgedBy(),
                entity.getAcknowledgedAt()
        );
    }
}
