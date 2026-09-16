package com.hrm.employeemanagement.domain.scenario.recruitment;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.scenario.recruitment.InvalidSimulatedEmployeeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ScenarioSimulatedEmployee Domain Entity Tests")
class ScenarioSimulatedEmployeeTest {

    @Test
    @DisplayName("Tạo nhân sự giả định hợp lệ và tính toán đúng công suất giờ mô phỏng")
    void testCreate_Valid() {
        ScenarioSimulatedEmployee emp = ScenarioSimulatedEmployee.create(
                1L,
                "Nguyễn Văn Giả Định",
                10L,
                5L,
                new BigDecimal("40.00"),
                4,
                "Ghi chú",
                100L
        );

        assertNotNull(emp);
        assertEquals("Nguyễn Văn Giả Định", emp.getCandidateName());
        assertEquals(0, new BigDecimal("160.00").compareTo(emp.calculateSimulatedCapacityHours()));
    }

    @Test
    @DisplayName("Ném InvalidSimulatedEmployeeException khi tên trống")
    void testCreate_BlankName_ShouldThrow() {
        assertThrows(InvalidSimulatedEmployeeException.class, () ->
                ScenarioSimulatedEmployee.create(1L, "   ", 10L, null, new BigDecimal("40.00"), 4, null, 1L)
        );
    }

    @Test
    @DisplayName("Ném InvalidSimulatedEmployeeException khi giờ chuẩn vượt quá 80h/tuần")
    void testCreate_ExcessHours_ShouldThrow() {
        assertThrows(InvalidSimulatedEmployeeException.class, () ->
                ScenarioSimulatedEmployee.create(1L, "Dev", 10L, null, new BigDecimal("85.00"), 4, null, 1L)
        );
    }

    @Test
    @DisplayName("Ném InvalidSimulatedEmployeeException khi số tuần không hợp lệ")
    void testCreate_InvalidWeeks_ShouldThrow() {
        assertThrows(InvalidSimulatedEmployeeException.class, () ->
                ScenarioSimulatedEmployee.create(1L, "Dev", 10L, null, new BigDecimal("40.00"), 0, null, 1L)
        );
    }
}