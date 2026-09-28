package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

public class InvalidWeekFormatException extends RuntimeException {
    public InvalidWeekFormatException(String message) {
        super(message);
    }
}