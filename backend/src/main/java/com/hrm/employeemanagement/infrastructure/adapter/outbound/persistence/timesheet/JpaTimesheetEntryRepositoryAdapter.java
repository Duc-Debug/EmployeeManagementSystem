package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.port.outbound.timesheet.DeleteTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetEntryPort;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetEntryRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.TimesheetEntryJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@Component
public class JpaTimesheetEntryRepositoryAdapter implements LoadTimesheetEntryPort, SaveTimesheetEntryPort, DeleteTimesheetEntryPort {

    private final SpringDataTimesheetEntryRepository repository;
    private final SpringDataEmployeeRepository employeeRepository;

    public JpaTimesheetEntryRepositoryAdapter(
            SpringDataTimesheetEntryRepository repository,
            SpringDataEmployeeRepository employeeRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public Optional<TimesheetEntry> findById(TimesheetEntryId id) {
        if (id == null || id.value() == null) return Optional.empty();
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<TimesheetEntry> findByTimesheetId(TimesheetId timesheetId) {
        if (timesheetId == null || timesheetId.value() == null) return List.of();
        return repository.findByTimesheetId(timesheetId.value()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimesheetEntry> findByEmployeeAndDateRange(EmployeeId employeeId, LocalDate startDate, LocalDate endDate) {
        if (employeeId == null || employeeId.value() == null || startDate == null || endDate == null) return List.of();
        return repository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAscCreatedAtAsc(employeeId.value(), startDate, endDate)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimesheetEntry> findPendingApprovals(EmployeeId managerId) {
        if (managerId == null || managerId.value() == null) return List.of();
        return repository.findPendingApprovalsByManager(managerId.value(), TimesheetStatus.SUBMITTED.name())
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumHoursByEmployeeAndDate(EmployeeId employeeId, LocalDate workDate, TimesheetEntryId excludeEntryId) {
        if (employeeId == null || employeeId.value() == null || workDate == null) return BigDecimal.ZERO;
        Long excludeId = excludeEntryId != null ? excludeEntryId.value() : null;
        BigDecimal sum = repository.sumHoursByEmployeeIdAndWorkDate(employeeId.value(), workDate, excludeId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public TimesheetEntry save(TimesheetEntry entry) {
        // Repository-level safety net: every current or future TimesheetEntry writer
        // uses this port, so none can bypass the employee-row serialization contract.
        employeeRepository.findByIdForUpdate(entry.getEmployeeIdValue())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên của dòng giờ công"));

        Long excludeId = entry.getId() != null ? entry.getIdValue() : null;
        BigDecimal existingDayHours = repository.sumHoursByEmployeeIdAndWorkDate(
                entry.getEmployeeIdValue(), entry.getWorkDate(), excludeId);
        existingDayHours = existingDayHours != null ? existingDayHours : BigDecimal.ZERO;
        if (existingDayHours.add(entry.getHours()).compareTo(BigDecimal.valueOf(12)) > 0) {
            throw new DailyHoursLimitExceededException(entry.getWorkDate(), existingDayHours, entry.getHours());
        }

        TimesheetEntryJpaEntity entity = toJpaEntity(entry);
        TimesheetEntryJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public void deleteById(TimesheetEntryId id) {
        if (id != null && id.value() != null) {
            repository.deleteById(id.value());
        }
    }

    private TimesheetEntry toDomain(TimesheetEntryJpaEntity entity) {
        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(entity.getId()),
                new TimesheetId(entity.getTimesheetId()),
                new EmployeeId(entity.getEmployeeId()),
                new ProjectId(entity.getProjectId()),
                new TaskId(entity.getTaskId()),
                entity.getWorkDate(),
                entity.getHours(),
                entity.getBillable() != null ? entity.getBillable() : true,
                entity.getDescription(),
                TimesheetStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
        entry.setRejectionReason(entity.getRejectionReason());
        return entry;
    }

    private TimesheetEntryJpaEntity toJpaEntity(TimesheetEntry domain) {
        TimesheetEntryJpaEntity entity = new TimesheetEntryJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getIdValue());
        }
        if (domain.getTimesheetId() != null) {
            entity.setTimesheetId(domain.getTimesheetIdValue());
        }
        entity.setEmployeeId(domain.getEmployeeIdValue());
        entity.setProjectId(domain.getProjectIdValue());
        entity.setTaskId(domain.getTaskIdValue());
        entity.setWorkDate(domain.getWorkDate());
        entity.setHours(domain.getHours());
        entity.setBillable(domain.isBillable());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus().name());
        entity.setRejectionReason(domain.getRejectionReason());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        entity.setUpdatedAt(domain.getUpdatedAt());
        if (domain.getVersion() != null) {
            entity.setVersion(domain.getVersion());
        }
        return entity;
    }
}
