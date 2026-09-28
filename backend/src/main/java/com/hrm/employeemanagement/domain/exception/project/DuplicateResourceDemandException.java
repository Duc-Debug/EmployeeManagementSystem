package com.hrm.employeemanagement.domain.exception.project;

public class DuplicateResourceDemandException extends RuntimeException {

    public DuplicateResourceDemandException(String message) {
        super(message);
    }
}