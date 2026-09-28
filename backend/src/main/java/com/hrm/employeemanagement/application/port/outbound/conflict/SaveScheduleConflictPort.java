package com.hrm.employeemanagement.application.port.outbound.conflict;

import java.util.List;

import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;

public interface SaveScheduleConflictPort {

    ScheduleConflict save(ScheduleConflict conflict);

    List<ScheduleConflict> saveAll(List<ScheduleConflict> conflicts);

    void deleteById(Long id);
}
