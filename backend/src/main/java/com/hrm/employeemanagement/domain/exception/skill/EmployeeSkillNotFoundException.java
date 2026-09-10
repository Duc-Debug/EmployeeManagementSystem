package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class EmployeeSkillNotFoundException extends DomainException {

    public EmployeeSkillNotFoundException(String message) {
        super(message);
    }
}