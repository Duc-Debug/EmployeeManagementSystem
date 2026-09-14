package com.hrm.employeemanagement.application.port.inbound.conflict;

import java.util.List;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;

public interface ScanScheduleConflictsUseCase {
    List<ScheduleConflictResult> scanScheduleConflicts(Integer yearNumber, Integer startWeek, Integer endWeek);
}
