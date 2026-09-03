package com.hrm.employeemanagement.application.dto.availability;

import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;

import java.math.BigDecimal;

public record WeeklyAvailabilityResult(
        Long employeeId,
        int year,
        int weekNumber,
        int standardHours,
        int holidayHours,
        BigDecimal approvedLeaveHours,
        BigDecimal netAvailableHours,
        Long version
) {
    public WeeklyAvailabilityResult(Long employeeId, int year, int weekNumber, int standardHours,
                                  int holidayHours, BigDecimal approvedLeaveHours, BigDecimal netAvailableHours) {
        this(employeeId, year, weekNumber, standardHours, holidayHours, approvedLeaveHours, netAvailableHours, 0L);
    }

    public static WeeklyAvailabilityResult fromDomain(WeeklyAvailability domain) {
        return new WeeklyAvailabilityResult(
                domain.getEmployeeId(),
                domain.getYear(),
                domain.getWeekNumber(),
                domain.getStandardHours(),
                domain.getHolidayHours(),
                domain.getApprovedLeaveHours(),
                domain.getNetAvailableHours(),
                domain.getVersion()
        );
    }
}
