package com.hrm.employeemanagement.domain.exception.user;

public class SelfLockingException extends RuntimeException {
    public SelfLockingException(String message) {
        super(message);
    }
}
