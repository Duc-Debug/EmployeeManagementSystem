package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateEmployeeSkillException extends DomainException {

    public DuplicateEmployeeSkillException(String message) {
        super(message);
    }
}
