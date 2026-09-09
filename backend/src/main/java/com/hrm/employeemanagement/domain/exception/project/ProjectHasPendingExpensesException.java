package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ProjectHasPendingExpensesException extends DomainException {
    public ProjectHasPendingExpensesException(String message) {
        super(message);
    }
}