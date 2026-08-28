package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class SkillNotFoundException extends DomainException {
    public SkillNotFoundException(String message) {
        super(message);
    }
}
