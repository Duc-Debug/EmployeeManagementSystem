package com.hrm.employeemanagement.domain.project.demand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidResourceDemandException;

@DisplayName("ProjectResourceDemandPolicy Domain Tests")
class ProjectResourceDemandPolicyTest {

    @Test
    @DisplayName("TC-01: Phân rã chính xác 8 tuần cho dự án kéo dài 8 tuần")
    void calculateProjectWeeks_8Weeks() {
        // Given: Dự án 8 tuần từ Thứ Hai 05/10/2026 đến Chủ Nhật 29/11/2026
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 11, 29);

        // When
        List<YearWeek> weeks = ProjectResourceDemandPolicy.calculateProjectWeeks(startDate, endDate);

        // Then: Đúng 8 tuần từ tuần 41 đến tuần 48
        assertEquals(8, weeks.size());
        assertEquals(YearWeek.of(2026, 41), weeks.get(0));
        assertEquals(YearWeek.of(2026, 48), weeks.get(7));
    }

    @Test
    @DisplayName("Edge Case: Phân rã tuần chính xác khi dự án kéo dài vắt qua năm mới")
    void calculateProjectWeeks_CrossYear() {
        // Given: Dự án từ 21/12/2026 đến 10/01/2027 (3 tuần)
        LocalDate startDate = LocalDate.of(2026, 12, 21);
        LocalDate endDate = LocalDate.of(2027, 1, 10);

        // When
        List<YearWeek> weeks = ProjectResourceDemandPolicy.calculateProjectWeeks(startDate, endDate);

        // Then
        assertEquals(3, weeks.size());
        assertEquals(YearWeek.of(2026, 52), weeks.get(0));
        assertEquals(YearWeek.of(2026, 53), weeks.get(1));
        assertEquals(YearWeek.of(2027, 1), weeks.get(2));
    }

    @Test
    @DisplayName("Ném InvalidProjectDateRangeException khi endDate trước startDate")
    void calculateProjectWeeks_InvalidRange() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 1);

        assertThrows(InvalidProjectDateRangeException.class,
                () -> ProjectResourceDemandPolicy.calculateProjectWeeks(startDate, endDate));
    }

    @Test
    @DisplayName("Validation giờ: Ném ngoại lệ khi số giờ <= 0 hoặc > 168 hoặc scale > 2")
    void validateRequiredHours_InvalidCases() {
        assertThrows(InvalidResourceDemandException.class,
                () -> ProjectResourceDemandPolicy.validateRequiredHours(null));
        assertThrows(InvalidResourceDemandException.class,
                () -> ProjectResourceDemandPolicy.validateRequiredHours(BigDecimal.ZERO));
        assertThrows(InvalidResourceDemandException.class,
                () -> ProjectResourceDemandPolicy.validateRequiredHours(new BigDecimal("-5.00")));
        assertThrows(InvalidResourceDemandException.class,
                () -> ProjectResourceDemandPolicy.validateRequiredHours(new BigDecimal("168.01")));
        assertThrows(InvalidResourceDemandException.class,
                () -> ProjectResourceDemandPolicy.validateRequiredHours(new BigDecimal("20.123")));
    }

    @Test
    @DisplayName("TC-02: Kiểm tra phát hiện vượt ngân sách tổng giờ dự kiến")
    void isExceedingBudget() {
        BigDecimal estimatedHours = new BigDecimal("100.00");
        BigDecimal totalDemandHours = new BigDecimal("160.00");

        assertTrue(ProjectResourceDemandPolicy.isExceedingBudget(totalDemandHours, estimatedHours));
        assertFalse(ProjectResourceDemandPolicy.isExceedingBudget(new BigDecimal("100.00"), estimatedHours));
        assertFalse(ProjectResourceDemandPolicy.isExceedingBudget(new BigDecimal("80.00"), estimatedHours));
    }
}