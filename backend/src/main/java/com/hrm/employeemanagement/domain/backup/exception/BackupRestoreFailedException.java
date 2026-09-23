package com.hrm.employeemanagement.domain.backup.exception;

public class BackupRestoreFailedException extends RuntimeException {
    public BackupRestoreFailedException(String message) {
        super(message);
    }

    public BackupRestoreFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
