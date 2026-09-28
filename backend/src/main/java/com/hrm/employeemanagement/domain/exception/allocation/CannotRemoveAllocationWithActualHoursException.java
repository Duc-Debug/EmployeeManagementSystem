package com.hrm.employeemanagement.domain.exception.allocation;

import com.hrm.employeemanagement.domain.exception.DomainException;

/**
 * Ngoại lệ ném ra khi cố gắng gỡ dòng phân bổ cho tuần đã trôi qua và đã có giờ công thực tế (TC-02).
 */
public class CannotRemoveAllocationWithActualHoursException extends DomainException {

    private final Long allocationId;
    private final int year;
    private final int weekNumber;

    public CannotRemoveAllocationWithActualHoursException(Long allocationId, int year, int weekNumber) {
        super(String.format(
                "Không thể gỡ dòng phân bổ (ID: %d) vì tuần %d/%d đã trôi qua và đã ghi nhận giờ công thực tế. Vui lòng ghi chú lý do chênh lệch thay vì xóa.",
                allocationId, weekNumber, year
        ));
        this.allocationId = allocationId;
        this.year = year;
        this.weekNumber = weekNumber;
    }

    public Long getAllocationId() {
        return allocationId;
    }

    public int getYear() {
        return year;
    }

    public int getWeekNumber() {
        return weekNumber;
    }
}
