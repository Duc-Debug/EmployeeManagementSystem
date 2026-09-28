package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ném ra khi người dùng cố gắng thêm, sửa hoặc xóa một phân bổ thuộc kỳ kế hoạch đã bị khóa (QTN-18 / TC-02).
 */
public class AllocationPeriodLockedException extends DomainException {

    private final String periodName;
    private final int year;
    private final int weekNumber;

    public AllocationPeriodLockedException(String periodName, int year, int weekNumber) {
        super("Không thể thay đổi phân bổ: Kỳ kế hoạch '" + periodName + "' (Tuần " + weekNumber + "/" + year 
                + ") đã bị khóa. Vui lòng mở lại kỳ trước khi thực hiện điều chỉnh theo quy tắc QTN-18.");
        this.periodName = periodName;
        this.year = year;
        this.weekNumber = weekNumber;
    }

    public AllocationPeriodLockedException(String message) {
        super(message);
        this.periodName = null;
        this.year = 0;
        this.weekNumber = 0;
    }

    public String getPeriodName() {
        return periodName;
    }

    public int getYear() {
        return year;
    }

    public int getWeekNumber() {
        return weekNumber;
    }
}
