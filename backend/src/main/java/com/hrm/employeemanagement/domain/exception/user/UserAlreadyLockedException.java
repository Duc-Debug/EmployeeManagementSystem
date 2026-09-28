package com.hrm.employeemanagement.domain.exception.user;

public class UserAlreadyLockedException extends RuntimeException {
    public UserAlreadyLockedException(String message) {
        super(message);
    }
}
