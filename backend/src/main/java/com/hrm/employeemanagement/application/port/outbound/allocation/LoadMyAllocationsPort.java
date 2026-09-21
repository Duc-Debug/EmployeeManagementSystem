package com.hrm.employeemanagement.application.port.outbound.allocation;

import java.time.LocalDate;
import java.util.List;
import com.hrm.employeemanagement.domain.allocation.confirmation.AllocationItem;

public interface LoadMyAllocationsPort {

    List<AllocationItem> loadAllocationsForEmployeeInWeek(Long employeeId, LocalDate weekStartDate);

    List<AllocationItem> loadAllocationsForEmployeeInWeeks(Long employeeId, List<LocalDate> weekStartDates);
}