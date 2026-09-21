package com.hrm.employeemanagement.application.dto.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MyWeeklyAllocationsResult(
        List<WeekScheduleDto> weeks
) {
    public record WeekScheduleDto(
            LocalDate weekStartDate,
            BigDecimal totalHours,
            String confirmationStatus,
            LocalDateTime confirmedAt,
            List<AllocationItemDto> allocations
    ) {}

    public record AllocationItemDto(
            Long allocationId,
            Long projectId,
            String projectName,
            String projectStatus,
            BigDecimal allocatedHours
    ) {}
}