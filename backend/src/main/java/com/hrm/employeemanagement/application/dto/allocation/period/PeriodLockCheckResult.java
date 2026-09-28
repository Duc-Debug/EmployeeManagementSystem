package com.hrm.employeemanagement.application.dto.allocation.period;

public record PeriodLockCheckResult(
        boolean isLocked,
        Long periodId,
        String periodName,
        int year,
        int weekNumber,
        String message
) {
    public static PeriodLockCheckResult unlocked(int year, int weekNumber) {
        return new PeriodLockCheckResult(false, null, null, year, weekNumber, "Tuần này chưa bị khóa kế hoạch, có thể phân bổ bình thường.");
    }

    public static PeriodLockCheckResult locked(Long periodId, String periodName, int year, int weekNumber) {
        return new PeriodLockCheckResult(
                true,
                periodId,
                periodName,
                year,
                weekNumber,
                "Tuần này thuộc kỳ kế hoạch '" + periodName + "' đã bị khóa. Cần mở lại kỳ trước khi thay đổi phân bổ theo QTN-18."
        );
    }
}
