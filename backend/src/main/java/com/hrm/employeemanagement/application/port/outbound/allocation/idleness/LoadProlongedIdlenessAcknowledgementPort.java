package com.hrm.employeemanagement.application.port.outbound.allocation.idleness;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.allocation.idleness.ProlongedIdlenessAcknowledgement;

public interface LoadProlongedIdlenessAcknowledgementPort {
    Optional<ProlongedIdlenessAcknowledgement> findByEmployeeAndPeriod(Long employeeId, int fromYear, int fromWeek, int durationWeeks);
    List<ProlongedIdlenessAcknowledgement> findByPeriodAndEmployees(int fromYear, int fromWeek, int durationWeeks, List<Long> employeeIds);
}
