package com.hrm.employeemanagement.domain.exception.user;

/**
 * Thrown when a password does not meet validation or current password verification fails.
 * Pure Java Domain Exception (Zero framework dependencies).
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
