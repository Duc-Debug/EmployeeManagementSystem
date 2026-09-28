package com.hrm.employeemanagement.application.port.inbound.reservation;

import com.hrm.employeemanagement.application.dto.reservation.CreateReservationCommand;
import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;

public interface CreateResourceReservationUseCase {
    ResourceReservationResult createReservation(CreateReservationCommand command);
}
