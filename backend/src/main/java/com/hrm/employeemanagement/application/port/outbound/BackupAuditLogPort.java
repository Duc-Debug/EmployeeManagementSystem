package com.hrm.employeemanagement.application.port.outbound;

import com.hrm.employeemanagement.domain.backup.BackupAuditLog;

import java.util.List;

public interface BackupAuditLogPort {
    BackupAuditLog save(BackupAuditLog log);
    List<BackupAuditLog> findAll();
    List<BackupAuditLog> findRecent(int limit);
}
