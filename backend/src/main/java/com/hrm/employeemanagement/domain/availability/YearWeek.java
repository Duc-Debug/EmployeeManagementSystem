package com.hrm.employeemanagement.domain.availability;

import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

public record YearWeek(int year, int weekNumber) {

    public YearWeek {
        if (year < 2000 || year > 2100) {
            throw new InvalidWeekNumberException("Năm không hợp lệ: " + year + ". Năm phải nằm trong khoảng từ 2000 đến 2100");
        }
        int maxWeeks = maxWeeksInYear(year);
        if (weekNumber < 1 || weekNumber > maxWeeks) {
            throw new InvalidWeekNumberException("Số tuần không hợp lệ: " + weekNumber + ". Năm " + year + " chỉ có " + maxWeeks + " tuần");
        }
    }

    /**
     * Tính tổng số tuần ISO-8601 trong một năm (52 hoặc 53 tuần).
     * Theo chuẩn ISO-8601, tuần chứa ngày 28 tháng 12 luôn là tuần cuối cùng của năm theo tuần.
     */
    public static int maxWeeksInYear(int year) {
        return LocalDate.of(year, 12, 28).get(WeekFields.ISO.weekOfWeekBasedYear());
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
