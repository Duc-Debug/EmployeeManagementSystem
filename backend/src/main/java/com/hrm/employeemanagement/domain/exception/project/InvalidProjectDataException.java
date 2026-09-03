package com.hrm.employeemanagement.domain.exception.project;
import com.hrm.employeemanagement.domain.exception.DomainException;
public class InvalidProjectDataException extends DomainException {
    public InvalidProjectDataException(String message) {
        super(message);
    }
    public InvalidProjectDataException(String message, Throwable cause) {
        super(message, cause);
    }
}