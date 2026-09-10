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
        Objects.requireNonNull(dayOfWeek, "Thứ trong tuần không được null");
    }
}
