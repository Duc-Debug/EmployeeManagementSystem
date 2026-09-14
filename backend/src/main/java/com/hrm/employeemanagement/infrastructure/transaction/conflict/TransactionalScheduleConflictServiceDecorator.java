package com.hrm.employeemanagement.infrastructure.transaction.conflict;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.NotifyScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ScanScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.service.conflict.ScheduleConflictService;

@Component
@Primary
public class TransactionalScheduleConflictServiceDecorator implements
        GetScheduleConflictsUseCase,
        ScanScheduleConflictsUseCase,
        NotifyScheduleConflictUseCase,
        ResolveScheduleConflictUseCase {

    private final ScheduleConflictService delegate;

    public TransactionalScheduleConflictServiceDecorator(ScheduleConflictService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public List<ScheduleConflictResult> getScheduleConflicts(ScheduleConflictQuery query) {
        return delegate.getScheduleConflicts(query);
    }

    @Override
    @Transactional
    public List<ScheduleConflictResult> scanScheduleConflicts(Integer yearNumber, Integer startWeek, Integer endWeek) {
        return delegate.scanScheduleConflicts(yearNumber, startWeek, endWeek);
    }

    @Override
    @Transactional
    public ScheduleConflictResult notifyScheduleConflict(Long conflictId) {
        return delegate.notifyScheduleConflict(conflictId);
    }

    @Override
    @Transactional
    public ScheduleConflictResult resolveScheduleConflict(Long conflictId) {
        return delegate.resolveScheduleConflict(conflictId);
    }
}
