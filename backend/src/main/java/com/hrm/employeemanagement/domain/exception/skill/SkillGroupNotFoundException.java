package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class SkillGroupNotFoundException extends DomainException {
    public SkillGroupNotFoundException(String message) {
        super(message);
    }
}
