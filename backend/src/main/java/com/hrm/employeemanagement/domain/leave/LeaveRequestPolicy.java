package com.hrm.employeemanagement.domain.leave;

import com.hrm.employeemanagement.domain.exception.leave.InvalidLeaveDateRangeException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Quy tắc nghiệp vụ cốt lõi cho đơn xin nghỉ phép (NCL-05-CN-002).
 */
public class LeaveRequestPolicy {

    public static final BigDecimal DEFAULT_HOURS_PER_DAY = BigDecimal.valueOf(8.00);

    /**
     * TC-03: Kiểm tra tính hợp lệ của khoảng ngày xin nghỉ.
     */
    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw InvalidLeaveDateRangeException.emptyDates();
        }
        if (endDate.isBefore(startDate)) {
            throw InvalidLeaveDateRangeException.endBeforeStart();
        }
    }

    /**
     * Đếm số ngày làm việc hành chính (Thứ 2 đến Thứ 6) trong khoảng ngày nghỉ.
     */
    public static int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        int count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    /**
     * Tính tổng số giờ nghỉ bị trừ dựa trên số ngày làm việc và giờ chuẩn/tuần của nhân sự.
     */
    public static BigDecimal calculateHoursDeducted(int workingDays, Integer standardHoursPerWeek) {
        if (workingDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal dailyHours = DEFAULT_HOURS_PER_DAY;
        if (standardHoursPerWeek != null && standardHoursPerWeek > 0) {
            dailyHours = BigDecimal.valueOf(standardHoursPerWeek)
                    .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
        }
        return dailyHours.multiply(BigDecimal.valueOf(workingDays)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * TC-02: Kiểm tra xem hai khoảng thời gian có bị giao nhau (trùng ngày) hay không.
     */
    public static boolean isOverlapping(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        return !startA.isAfter(endB) && !endA.isBefore(startB);
    }
}
