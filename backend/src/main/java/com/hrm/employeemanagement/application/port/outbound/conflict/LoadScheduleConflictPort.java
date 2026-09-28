package com.hrm.employeemanagement.application.port.outbound.conflict;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;

public interface LoadScheduleConflictPort {

    Optional<ScheduleConflict> findById(Long id);

    List<ScheduleConflict> findConflicts(
            Integer yearNumber,
            Integer startWeek,
            Integer endWeek,
            Long employeeId,
            ConflictType conflictType,
            ScheduleConflictStatus status
    );

    Optional<ScheduleConflict> findExistingConflict(
            Long employeeId,
            Integer yearNumber,
            Integer weekNumber,
            ConflictType conflictType
    );

    List<ScheduleConflict> findUnresolvedConflictsForEmployees(
            List<Long> employeeIds,
            Integer startYear,
            Integer startWeek,
            Integer endYear,
            Integer endWeek
    );
}
