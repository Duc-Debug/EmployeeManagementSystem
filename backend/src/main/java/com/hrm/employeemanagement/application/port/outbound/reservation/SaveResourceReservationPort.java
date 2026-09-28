package com.hrm.employeemanagement.application.port.outbound.reservation;

import com.hrm.employeemanagement.domain.reservation.ResourceReservation;

import java.util.List;

public interface SaveResourceReservationPort {
    ResourceReservation save(ResourceReservation reservation);
    List<ResourceReservation> saveAll(List<ResourceReservation> reservations);
}
