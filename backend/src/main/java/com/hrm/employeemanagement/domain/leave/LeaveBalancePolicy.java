package com.hrm.employeemanagement.domain.leave;

import com.hrm.employeemanagement.domain.exception.leave.LeaveBalanceExceededException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quy tắc nghiệp vụ tính quỹ ngày phép còn lại (NCL-05-CN-005).
 */
public class LeaveBalancePolicy {

    /**
     * TC-01: Tính số ngày phép năm còn lại khả dụng:
     * Còn lại = (Được hưởng + Năm trước chuyển sang) - Đã nghỉ (APPROVED) - Đang chờ duyệt (PENDING)
     */
    public static BigDecimal calculateRemainingDays(
            BigDecimal entitledDays,
            BigDecimal carriedOverDays,
            BigDecimal usedDays,
            BigDecimal pendingDays
    ) {
        BigDecimal safeEntitled = entitledDays != null ? entitledDays : BigDecimal.ZERO;
        BigDecimal safeCarried = carriedOverDays != null ? carriedOverDays : BigDecimal.ZERO;
        BigDecimal safeUsed = usedDays != null ? usedDays : BigDecimal.ZERO;
        BigDecimal safePending = pendingDays != null ? pendingDays : BigDecimal.ZERO;

        BigDecimal totalAvailableFund = safeEntitled.add(safeCarried);
        BigDecimal totalDeducted = safeUsed.add(safePending);
        BigDecimal remaining = totalAvailableFund.subtract(totalDeducted);

        return remaining.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * TC-02: Kiểm tra xem số ngày xin nghỉ mới có vượt quá quỹ phép năm còn lại hay không.
     * Nếu vượt quá => Ném ra ngoại lệ LeaveBalanceExceededException.
     */
    public static void validateSufficientBalance(BigDecimal remainingDays, BigDecimal requestedDays) {
        BigDecimal safeRemaining = remainingDays != null ? remainingDays : BigDecimal.ZERO;
        BigDecimal safeRequested = requestedDays != null ? requestedDays : BigDecimal.ZERO;

        if (safeRequested.compareTo(safeRemaining) > 0) {
            throw new LeaveBalanceExceededException(safeRemaining, safeRequested);
        }
    }

    /**
     * Tính số ngày làm việc thuộc về một năm cụ thể cho đơn xin nghỉ phép có thể vắt qua nhiều năm.
     * Ví dụ: Đơn từ 2026-12-30 đến 2027-01-05.
     * Khi tính cho năm 2026: Khoảng giao là 2026-12-30 -> 2026-12-31.
     * Khi tính cho năm 2027: Khoảng giao là 2027-01-01 -> 2027-01-05.
     */
    public static int calculateWorkingDaysInYear(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            int targetYear,
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar,
            java.util.Set<java.time.LocalDate> holidayDates
    ) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        java.time.LocalDate yearStart = java.time.LocalDate.of(targetYear, 1, 1);
        java.time.LocalDate yearEnd = java.time.LocalDate.of(targetYear, 12, 31);

        if (endDate.isBefore(yearStart) || startDate.isAfter(yearEnd)) {
            return 0;
        }

        java.time.LocalDate effectiveStart = startDate.isBefore(yearStart) ? yearStart : startDate;
        java.time.LocalDate effectiveEnd = endDate.isAfter(yearEnd) ? yearEnd : endDate;

        return LeaveRequestPolicy.calculateWorkingDays(effectiveStart, effectiveEnd, calendar, holidayDates);
    }

    public static int calculateWorkingDaysInYear(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            int targetYear
    ) {
        return calculateWorkingDaysInYear(startDate, endDate, targetYear, null, java.util.Collections.emptySet());
    }
}
