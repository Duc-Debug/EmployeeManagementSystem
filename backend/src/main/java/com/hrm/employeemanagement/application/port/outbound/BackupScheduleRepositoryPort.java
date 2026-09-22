package com.hrm.employeemanagement.application.port.outbound;

import com.hrm.employeemanagement.domain.backup.BackupSchedule;

import java.util.Optional;

public interface BackupScheduleRepositoryPort {
    BackupSchedule save(BackupSchedule schedule);
    Optional<BackupSchedule> findSchedule();
}
