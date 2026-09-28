package com.hrm.employeemanagement.application.port.inbound.reservation;

import com.hrm.employeemanagement.application.dto.reservation.ResourceReservationResult;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;

import java.util.List;

public interface GetResourceReservationsUseCase {
    List<ResourceReservationResult> getReservations(Long projectId, Long employeeId, Integer year, Integer weekNumber, ReservationStatus status);
}
