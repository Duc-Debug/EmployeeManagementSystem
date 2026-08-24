package com.hrm.employeemanagement.domain.exception.user;

/**
 * Thrown when a password reset token is invalid, expired, or already used.
 * Pure Java Domain Exception (Zero framework dependencies).
 */
public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException(String message) {
        super(message);
    }
}
