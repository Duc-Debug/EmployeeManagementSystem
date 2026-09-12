package com.hrm.employeemanagement.application.port.outbound.reservation;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.reservation.ReservationStatus;
import com.hrm.employeemanagement.domain.reservation.ResourceReservation;

import java.util.List;
import java.util.Optional;

public interface LoadResourceReservationPort {
    Optional<ResourceReservation> findById(Long id);
    Optional<ResourceReservation> findActiveByProjectAndEmployeeAndYearWeek(Long projectId, Long employeeId, YearWeek yearWeek);
    List<ResourceReservation> findActiveByEmployeeIdAndYearWeek(Long employeeId, YearWeek yearWeek);
    List<ResourceReservation> findActiveByEmployeeIdsAndYearWeeks(List<Long> employeeIds, List<YearWeek> yearWeeks);
    List<ResourceReservation> findByProjectId(Long projectId);
    List<ResourceReservation> findActiveByProjectId(Long projectId);
    List<ResourceReservation> findReservations(Long projectId, Long employeeId, Integer year, Integer weekNumber, ReservationStatus status);
}
