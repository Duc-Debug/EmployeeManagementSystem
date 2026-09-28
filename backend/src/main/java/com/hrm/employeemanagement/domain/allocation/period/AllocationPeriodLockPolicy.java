package com.hrm.employeemanagement.domain.allocation.period;

import java.util.List;
import java.util.Optional;

import com.hrm.employeemanagement.domain.exception.allocation.AllocationPeriodLockedException;

/**
 * Domain Policy thực thi quy tắc QTN-18: Khóa kế hoạch phân bổ của kỳ đã chốt.
 * Khi một kỳ kế hoạch đã khóa thì không được thêm, sửa hay gỡ phân bổ thuộc kỳ đó.
 */
public final class AllocationPeriodLockPolicy {

    private AllocationPeriodLockPolicy() {
    }

    /**
     * Tìm kỳ đang bị khóa bao phủ tuần/năm được chỉ định.
     */
    public static Optional<AllocationPlanningPeriod> findLockedPeriodCoveringWeek(
            int year,
            int weekNumber,
            List<AllocationPlanningPeriod> periods
    ) {
        if (periods == null || periods.isEmpty()) {
            return Optional.empty();
        }
        return periods.stream()
                .filter(AllocationPlanningPeriod::isLocked)
                .filter(p -> p.isWeekWithin(year, weekNumber))
                .findFirst();
    }

    /**
     * Kiểm tra tuần/năm có thuộc kỳ nào đang bị khóa hay không.
     */
    public static boolean isWeekLocked(int year, int weekNumber, List<AllocationPlanningPeriod> periods) {
        return findLockedPeriodCoveringWeek(year, weekNumber, periods).isPresent();
    }

    /**
     * Xác thực xem có được phép thay đổi phân bổ cho tuần/năm hay không theo QTN-18.
     * Ném AllocationPeriodLockedException nếu tuần nằm trong một kỳ đã bị khóa (TC-02).
     */
    public static void validateCanModifyAllocation(int year, int weekNumber, List<AllocationPlanningPeriod> periods) {
        findLockedPeriodCoveringWeek(year, weekNumber, periods).ifPresent(lockedPeriod -> {
            throw new AllocationPeriodLockedException(lockedPeriod.getName(), year, weekNumber);
        });
    }
}
