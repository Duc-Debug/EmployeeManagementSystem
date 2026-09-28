package com.hrm.employeemanagement.domain.unavailability;

import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public class UnavailabilityPolicy {

    public static final int DEFAULT_HOURS_PER_WORKING_DAY = 8;
    public static final Set<DayOfWeek> DEFAULT_WORKING_DAYS = Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
    );

    public static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidUnavailabilityPeriodException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidUnavailabilityPeriodException("Ngày bắt đầu không được sau ngày kết thúc");
        }
    }

    public static int countWorkingDays(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> workingDays) {
        validatePeriod(startDate, endDate);
        Set<DayOfWeek> effectiveWorkingDays = (workingDays != null && !workingDays.isEmpty())
                ? workingDays
                : DEFAULT_WORKING_DAYS;

        int count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (effectiveWorkingDays.contains(current.getDayOfWeek())) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    public static BigDecimal calculateTotalHours(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> workingDays) {
        return calculateTotalHours(startDate, endDate, workingDays, DEFAULT_HOURS_PER_WORKING_DAY);
    }

    public static BigDecimal calculateTotalHours(LocalDate startDate, LocalDate endDate, Set<DayOfWeek> workingDays, int hoursPerDay) {
        int workingDaysCount = countWorkingDays(startDate, endDate, workingDays);
        return BigDecimal.valueOf((long) workingDaysCount * hoursPerDay).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tính số giờ không sẵn sàng rơi vào một khoảng thời gian cụ thể (ví dụ tuần làm việc [windowStart, windowEnd]).
     */
    public static BigDecimal calculateHoursInWindow(
            LocalDate declStart, LocalDate declEnd, BigDecimal totalHours,
            LocalDate windowStart, LocalDate windowEnd, Set<DayOfWeek> workingDays) {
        if (totalHours == null || totalHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (declStart == null || declEnd == null || windowStart == null || windowEnd == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (declStart.isAfter(windowEnd) || declEnd.isBefore(windowStart)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int totalWorkingDays = countWorkingDays(declStart, declEnd, workingDays);
        if (totalWorkingDays == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        LocalDate overlapStart = declStart.isAfter(windowStart) ? declStart : windowStart;
        LocalDate overlapEnd = declEnd.isBefore(windowEnd) ? declEnd : windowEnd;

        int overlapWorkingDays = countWorkingDays(overlapStart, overlapEnd, workingDays);
        if (overlapWorkingDays == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (overlapWorkingDays == totalWorkingDays) {
            return totalHours.setScale(2, RoundingMode.HALF_UP);
        }

        return totalHours
                .multiply(BigDecimal.valueOf(overlapWorkingDays))
                .divide(BigDecimal.valueOf(totalWorkingDays), 2, RoundingMode.HALF_UP);
    }
}
