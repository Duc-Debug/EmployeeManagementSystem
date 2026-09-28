package com.hrm.employeemanagement.domain.exception.reservation;

import com.hrm.employeemanagement.domain.exception.DomainException;

public class ReservationNotFoundException extends DomainException {
    public ReservationNotFoundException(String message) {
        super(message);
    }
}
