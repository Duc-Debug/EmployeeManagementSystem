package com.hrm.employeemanagement.application.port.outbound.unavailability;

import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoadUnavailabilityDeclarationPort {

    Optional<UnavailabilityDeclaration> findById(Long id);

    Optional<UnavailabilityDeclaration> findByIdForUpdate(Long id);

    List<UnavailabilityDeclaration> findByEmployeeId(Long employeeId);

    List<UnavailabilityDeclaration> findAllPending();

    List<UnavailabilityDeclaration> findPendingByOrgUnitIds(List<Long> orgUnitIds);

    List<UnavailabilityDeclaration> findActiveOverlapping(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<UnavailabilityDeclaration> findApprovedByEmployeeIdAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate);

    BigDecimal getTotalApprovedUnavailabilityHoursBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    void deleteById(Long id);
}
