package com.hrm.employeemanagement.domain.exception.user;

/**
 * Thrown when an authentication attempt fails due to invalid username or password.
 * Pure Java Domain Exception (Zero framework dependencies).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
