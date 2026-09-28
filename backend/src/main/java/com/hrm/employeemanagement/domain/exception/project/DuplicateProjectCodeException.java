package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class DuplicateProjectCodeException extends DomainException{
    public DuplicateProjectCodeException(String message){
        super(message);
    }
}
