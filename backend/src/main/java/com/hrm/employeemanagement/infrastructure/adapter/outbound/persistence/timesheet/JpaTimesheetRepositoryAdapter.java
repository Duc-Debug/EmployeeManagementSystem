package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.timesheet;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.TimesheetJpaEntity;

@Component
public class JpaTimesheetRepositoryAdapter implements LoadTimesheetPort, SaveTimesheetPort {

    private final SpringDataTimesheetRepository repository;

    public JpaTimesheetRepositoryAdapter(SpringDataTimesheetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Timesheet> findById(TimesheetId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Timesheet> findByEmployeeAndWeekStart(EmployeeId employeeId, LocalDate weekStartDate) {
        if (employeeId == null || employeeId.value() == null || weekStartDate == null) return Optional.empty();
        return repository.findByEmployeeIdAndWeekStartDate(employeeId.value(), weekStartDate).map(this::toDomain);
    }

    @Override
    public java.util.List<Timesheet> findDraftTimesheetsForReminderUpTo(LocalDate targetDate, int limit) {
        return repository.findByWeekStartDateLessThanEqualAndStatusAndRemindedAtIsNull(targetDate, TimesheetStatus.DRAFT.name(), org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Timesheet save(Timesheet timesheet) {
        TimesheetJpaEntity entity = toJpaEntity(timesheet);
        TimesheetJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private Timesheet toDomain(TimesheetJpaEntity entity) {
        Timesheet domain = new Timesheet(
                new TimesheetId(entity.getId()),
                new EmployeeId(entity.getEmployeeId()),
                entity.getWeekStartDate(),
                entity.getWeekEndDate(),
                entity.getTotalHours(),
                TimesheetStatus.valueOf(entity.getStatus()),
                entity.getSubmittedAt(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getRejectionReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                null
        );
        domain.setRemindedAt(entity.getRemindedAt());
        return domain;
    }

    private TimesheetJpaEntity toJpaEntity(Timesheet domain) {
        TimesheetJpaEntity entity = new TimesheetJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getIdValue());
        }
        entity.setEmployeeId(domain.getEmployeeIdValue());
        entity.setWeekStartDate(domain.getWeekStartDate());
        entity.setWeekEndDate(domain.getWeekEndDate());
        entity.setTotalHours(domain.getTotalHours());
        entity.setStatus(domain.getStatus().name());
        entity.setSubmittedAt(domain.getSubmittedAt());
        entity.setApprovedBy(domain.getApprovedBy());
        entity.setApprovedAt(domain.getApprovedAt());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setRemindedAt(domain.getRemindedAt());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        entity.setUpdatedAt(domain.getUpdatedAt());
        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
        }
        return entity;
    }

    @Override
    public void saveAll(java.util.List<Timesheet> timesheets) {
        java.util.List<TimesheetJpaEntity> entities = timesheets.stream()
                .map(this::toJpaEntity)
                .collect(java.util.stream.Collectors.toList());
        repository.saveAll(entities);
    }
}
