package com.hrm.employeemanagement.domain.availability;

import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Objects;

public record YearWeek(int year, int weekNumber) {

    public YearWeek {
        if (year < 2000 || year > 2100) {
            throw new InvalidWeekNumberException("Năm không hợp lệ: " + year + ". Năm phải nằm trong khoảng từ 2000 đến 2100");
        }
        if (weekNumber < 1 || weekNumber > 53) {
            throw new InvalidWeekNumberException("Số tuần không hợp lệ: " + weekNumber + ". Số tuần trong năm phải từ 1 đến 53");
        }
    }

    public static YearWeek of(int year, int weekNumber) {
        return new YearWeek(year, weekNumber);
    }

    /**
     * Ngày đầu tiên của tuần (Thứ Hai theo chuẩn ISO-8601).
     */
    public LocalDate getStartDate() {
        return LocalDate.of(year, 2, 1)
                .with(WeekFields.ISO.weekOfWeekBasedYear(), weekNumber)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Ngày cuối cùng của tuần làm việc / tuần dương lịch (Chủ Nhật theo chuẩn ISO-8601).
     */
    public LocalDate getEndDate() {
        return getStartDate().plusDays(6);
    }
}
