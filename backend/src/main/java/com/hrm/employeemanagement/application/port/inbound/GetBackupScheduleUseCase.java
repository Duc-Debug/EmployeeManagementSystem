package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.domain.backup.BackupSchedule;

public interface GetBackupScheduleUseCase {
    BackupSchedule getSchedule();
}
