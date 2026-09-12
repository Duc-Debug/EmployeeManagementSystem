package com.hrm.employeemanagement.application.port.inbound.reservation;

import com.hrm.employeemanagement.application.dto.reservation.CancelReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;

public interface CancelResourceReservationUseCase {
    ResourceReservationResult cancelReservation(CancelReservationCommand command);
}
