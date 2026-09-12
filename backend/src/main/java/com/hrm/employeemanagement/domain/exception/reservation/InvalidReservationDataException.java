package com.hrm.employeemanagement.domain.exception.reservation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class InvalidReservationDataException extends DomainException {
    public InvalidReservationDataException(String message) {
        super(message);
    }
}
