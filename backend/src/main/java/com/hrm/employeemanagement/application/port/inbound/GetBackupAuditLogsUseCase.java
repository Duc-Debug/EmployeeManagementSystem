package com.hrm.employeemanagement.application.port.inbound;

import com.hrm.employeemanagement.domain.backup.BackupAuditLog;

import java.util.List;

public interface GetBackupAuditLogsUseCase {
    List<BackupAuditLog> getAuditLogs();
}
