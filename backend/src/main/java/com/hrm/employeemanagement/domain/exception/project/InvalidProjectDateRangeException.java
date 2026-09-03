package com.hrm.employeemanagement.domain.exception.project;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidProjectDateRangeException extends DomainException {
    public InvalidProjectDateRangeException(String message) {
        super(message);
    }

    public static InvalidProjectDateRangeException invalidRange(){
return new InvalidProjectDateRangeException("Ngày kết thúc dự kiến không được sớm hơn ngày bắt đầu");
    }
}
