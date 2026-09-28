package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateSkillNameException extends DomainException {
    public DuplicateSkillNameException(String message) {
        super(message);
    }
}
