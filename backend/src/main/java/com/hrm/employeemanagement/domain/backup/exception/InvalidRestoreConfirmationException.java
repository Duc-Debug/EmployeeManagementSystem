package com.hrm.employeemanagement.domain.backup.exception;

public class InvalidRestoreConfirmationException extends RuntimeException {
    public InvalidRestoreConfirmationException(String message) {
        super(message);
    }
}
