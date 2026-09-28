package com.hrm.employeemanagement.domain.exception.skill;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidSkillMergeException extends DomainException {
    public InvalidSkillMergeException(String message) {
        super(message);
    }
}