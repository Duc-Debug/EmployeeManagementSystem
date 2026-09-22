package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.application.dto.backup.UpdateBackupScheduleRequest;
import com.hrm.employeemanagement.domain.backup.BackupSchedule;

public interface UpdateBackupScheduleUseCase {
    BackupSchedule updateSchedule(UpdateBackupScheduleRequest request, Long currentUserId, String currentUserEmail, String clientIp);
}
