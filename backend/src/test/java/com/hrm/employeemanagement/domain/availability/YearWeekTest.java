package com.hrm.employeemanagement.domain.availability;

import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("YearWeek Value Object Tests")
class YearWeekTest {

    @Test
    @DisplayName("Tạo YearWeek hợp lệ và tính ngày bắt đầu (Thứ 2) / kết thúc (Chủ Nhật)")
    void validYearWeek() {
        YearWeek yearWeek = YearWeek.of(2026, 36);

        assertEquals(2026, yearWeek.year());
        assertEquals(36, yearWeek.weekNumber());

        LocalDate startDate = yearWeek.getStartDate();
        LocalDate endDate = yearWeek.getEndDate();

        assertEquals(DayOfWeek.MONDAY, startDate.getDayOfWeek());
        assertEquals(DayOfWeek.SUNDAY, endDate.getDayOfWeek());
        assertEquals(6, startDate.until(endDate).getDays());
    }

    @Test
    @DisplayName("Ném InvalidWeekNumberException khi tuần hoặc năm không hợp lệ")
    void invalidWeekOrYear() {
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2026, 0));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2026, 54));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(1999, 10));
        assertThrows(InvalidWeekNumberException.class, () -> YearWeek.of(2101, 10));
    }
}
