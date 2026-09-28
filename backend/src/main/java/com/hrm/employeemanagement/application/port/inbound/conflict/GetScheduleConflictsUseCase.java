package com.hrm.employeemanagement.application.port.inbound.conflict;

import java.util.List;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;

public interface GetScheduleConflictsUseCase {
    List<ScheduleConflictResult> getScheduleConflicts(ScheduleConflictQuery query);
}
