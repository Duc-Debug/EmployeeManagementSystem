package com.hrm.employeemanagement.domain.exception.reservation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidReservationStateException extends DomainException {
    public InvalidReservationStateException(String message) {
        super(message);
    }
}
