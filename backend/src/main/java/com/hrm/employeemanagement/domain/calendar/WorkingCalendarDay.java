package com.hrm.employeemanagement.domain.calendar;

import java.time.DayOfWeek;
import java.util.Objects;

/**
 * Value Object biểu diễn cấu hình ngày làm việc cho một thứ trong tuần.
 */
public record WorkingCalendarDay(
        DayOfWeek dayOfWeek,
        boolean isWorkingDay
) {
    public WorkingCalendarDay {
        if (dayOfWeek == null) {
            throw new com.hrm.employeemanagement.domain.exception.calendar.InvalidWorkingCalendarException("Thứ trong tuần không được null");
        }
    }
}
