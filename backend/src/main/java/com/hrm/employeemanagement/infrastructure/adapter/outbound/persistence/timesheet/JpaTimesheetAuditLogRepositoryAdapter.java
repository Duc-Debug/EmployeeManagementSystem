package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.timesheet;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetAuditLogPort;
import com.hrm.employeemanagement.domain.timesheet.TimesheetAuditLog;
import com.hrm.employeemanagement.domain.timesheet.TimesheetAuditLogId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetAuditLogRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.TimesheetAuditLogJpaEntity;

@Component
public class JpaTimesheetAuditLogRepositoryAdapter implements SaveTimesheetAuditLogPort {

    private final SpringDataTimesheetAuditLogRepository repository;

    public JpaTimesheetAuditLogRepositoryAdapter(SpringDataTimesheetAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public TimesheetAuditLog save(TimesheetAuditLog auditLog) {
        TimesheetAuditLogJpaEntity entity = toJpaEntity(auditLog);
        TimesheetAuditLogJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private TimesheetAuditLog toDomain(TimesheetAuditLogJpaEntity entity) {
        return new TimesheetAuditLog(
                new TimesheetAuditLogId(entity.getId()),
                new TimesheetId(entity.getTimesheetId()),
                entity.getEntryId() != null ? new TimesheetEntryId(entity.getEntryId()) : null,
                entity.getAction(),
                new UserId(entity.getActorId()),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }

    private TimesheetAuditLogJpaEntity toJpaEntity(TimesheetAuditLog domain) {
        TimesheetAuditLogJpaEntity entity = new TimesheetAuditLogJpaEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getIdValue());
        }
        entity.setTimesheetId(domain.getTimesheetIdValue());
        entity.setEntryId(domain.getEntryIdValue());
        entity.setAction(domain.getAction());
        entity.setActorId(domain.getActorIdValue());
        entity.setNote(domain.getNote());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
