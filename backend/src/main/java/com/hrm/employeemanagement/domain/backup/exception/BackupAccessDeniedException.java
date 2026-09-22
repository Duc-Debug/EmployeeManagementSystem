package com.hrm.employeemanagement.domain.backup.exception;

public class BackupAccessDeniedException extends RuntimeException {
    public BackupAccessDeniedException(String message) {
        super(message);
    }
}
