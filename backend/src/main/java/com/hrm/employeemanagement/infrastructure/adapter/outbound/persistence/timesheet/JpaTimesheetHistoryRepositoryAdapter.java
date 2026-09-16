package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.timesheet;

import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetHistoryPort;
import com.hrm.employeemanagement.domain.timesheet.TimesheetHistory;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.SpringDataTimesheetHistoryRepository;
import com.hrm.employeemanagement.infrastructure.persistence.timesheet.TimesheetHistoryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class JpaTimesheetHistoryRepositoryAdapter implements SaveTimesheetHistoryPort {

    private final SpringDataTimesheetHistoryRepository repository;

    public JpaTimesheetHistoryRepositoryAdapter(SpringDataTimesheetHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(TimesheetHistory history) {
        TimesheetHistoryJpaEntity entity = new TimesheetHistoryJpaEntity();
        if (history.getId() != null) {
            entity.setId(history.getId());
        }
        entity.setTimesheetId(history.getTimesheetId().value());
        entity.setAction(history.getAction());
        entity.setActionBy(history.getActionBy() != null ? history.getActionBy().value() : null);
        entity.setContent(history.getContent());
        entity.setCreatedAt(history.getCreatedAt());
        
        repository.save(entity);
    }

    @Override
    public void saveAll(java.util.List<TimesheetHistory> histories) {
        java.util.List<TimesheetHistoryJpaEntity> entities = histories.stream().map(history -> {
            TimesheetHistoryJpaEntity entity = new TimesheetHistoryJpaEntity();
            if (history.getId() != null) {
                entity.setId(history.getId());
            }
            entity.setTimesheetId(history.getTimesheetId().value());
            entity.setAction(history.getAction());
            entity.setActionBy(history.getActionBy() != null ? history.getActionBy().value() : null);
            entity.setContent(history.getContent());
            entity.setCreatedAt(history.getCreatedAt());
            return entity;
        }).collect(java.util.stream.Collectors.toList());
        repository.saveAll(entities);
    }
}
