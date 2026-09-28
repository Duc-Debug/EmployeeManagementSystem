package com.hrm.employeemanagement.infrastructure.config;

import com.hrm.employeemanagement.application.port.outbound.BackupAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.BackupRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.BackupScheduleRepositoryPort;
import com.hrm.employeemanagement.application.port.outbound.BackupStoragePort;
import com.hrm.employeemanagement.application.port.outbound.DatabaseBackupRestoreEnginePort;
import com.hrm.employeemanagement.application.service.BackupService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackupUseCaseConfig {

    @Bean
    public BackupService backupService(
            BackupRepositoryPort backupRepositoryPort,
            BackupScheduleRepositoryPort backupScheduleRepositoryPort,
            BackupAuditLogPort backupAuditLogPort,
            BackupStoragePort backupStoragePort,
            DatabaseBackupRestoreEnginePort backupRestoreEnginePort
    ) {
        return new BackupService(
                backupRepositoryPort,
                backupScheduleRepositoryPort,
                backupAuditLogPort,
                backupStoragePort,
                backupRestoreEnginePort
        );
    }
}
