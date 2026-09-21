package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

public class WeekStartNotMondayException extends RuntimeException {
    public WeekStartNotMondayException(String message) {
        super(message);
    }
}
