package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

public class InvalidWeeksFormatException extends RuntimeException {
    public InvalidWeeksFormatException(String message) {
        super(message);
    }
}