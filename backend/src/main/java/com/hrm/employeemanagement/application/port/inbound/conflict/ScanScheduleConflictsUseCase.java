package com.hrm.employeemanagement.application.port.inbound.conflict;

import java.util.List;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;

/**
 * Command use case to scan project allocation and leave data to synchronize schedule conflict warnings.
 * <p>
 * Unlike read-only query use cases (e.g. {@link GetScheduleConflictsUseCase}), this is a write-side command
 * with database side-effects: it creates new conflict warnings, updates metrics on active warnings, and
 * reopens previously {@code RESOLVED} warnings if the underlying conflict condition still persists.
 */
public interface ScanScheduleConflictsUseCase {
    List<ScheduleConflictResult> scanScheduleConflicts(Integer yearNumber, Integer startWeek, Integer endWeek);
}
