package com.hrm.employeemanagement.domain.exception.user;

/**
 * Thrown when an authentication attempt is made against a locked or inactive account.
 * Pure Java Domain Exception (Zero framework dependencies).
 */
public class UserLockedException extends RuntimeException {
    public UserLockedException(String message) {
        super(message);
    }
}
