package com.hrm.employeemanagement.domain.backup.exception;

public class InvalidBackupStatusException extends RuntimeException {
    public InvalidBackupStatusException(String message) {
        super(message);
    }
}
