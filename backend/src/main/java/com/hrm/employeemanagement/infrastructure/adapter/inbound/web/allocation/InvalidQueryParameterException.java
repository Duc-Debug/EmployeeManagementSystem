package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

public class InvalidQueryParameterException extends RuntimeException {
    public InvalidQueryParameterException(String message) {
        super(message);
    }
}