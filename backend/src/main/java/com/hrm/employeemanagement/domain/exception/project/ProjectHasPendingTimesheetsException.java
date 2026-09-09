package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectHasPendingTimesheetsException extends DomainException {
    public ProjectHasPendingTimesheetsException(String message) {
        super(message);
    }
}