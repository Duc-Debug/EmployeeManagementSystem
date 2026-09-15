package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioDemandTest {

    @Test
    @DisplayName("Validation: headcount <= 0 ném InvalidScenarioDemandException")
    void testHeadcountLessThanOrEqualZero_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 0, 2026, 1, 2026, 4, BigDecimal.valueOf(40), "Java")
        );

        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", -2, 2026, 1, 2026, 4, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Validation: hoursPerWeekPerPerson <= 0 ném InvalidScenarioDemandException")
    void testHoursLessThanOrEqualZero_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 2026, 1, 2026, 4, BigDecimal.ZERO, "Java")
        );

        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 2026, 1, 2026, 4, BigDecimal.valueOf(-5), "Java")
        );
    }

    @Test
    @DisplayName("Validation: start > end ném InvalidScenarioDemandException")
    void testStartGreaterThanEnd_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 2026, 8, 2026, 4, BigDecimal.valueOf(40), "Java")
        );

        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 2027, 2, 2026, 2, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Validation: Tuần ngoài khoảng 1..53 ném InvalidScenarioDemandException")
    void testWeekOutOfRange_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 2026, 0, 2026, 4, BigDecimal.valueOf(40), "Java")
        );

        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 2026, 1, 2026, 54, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Validation: Tên nhu cầu rỗng ném InvalidScenarioDemandException")
    void testBlankDemandName_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "   ", 2, 2026, 1, 2026, 4, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Nhu cầu hợp lệ: Tính toán tổng giờ/tuần đúng (headcount * hours) và isActiveInWeek theo YearWeek")
    void testValidDemand_CalculatesTotalHoursCorrectly() {
        ScenarioDemand demand = ScenarioDemand.create(
                1L, "Tuyển Java Dev", 3, 2026, 2, 2026, 6, BigDecimal.valueOf(40), "Java Spring"
        );

        assertEquals(BigDecimal.valueOf(120), demand.getTotalHoursPerWeek());
        assertTrue(demand.isActiveInWeek(YearWeek.of(2026, 2)));
        assertTrue(demand.isActiveInWeek(YearWeek.of(2026, 4)));
        assertTrue(demand.isActiveInWeek(YearWeek.of(2026, 6)));
        assertFalse(demand.isActiveInWeek(YearWeek.of(2026, 1)));
        assertFalse(demand.isActiveInWeek(YearWeek.of(2026, 7)));
        assertFalse(demand.isActiveInWeek(YearWeek.of(2027, 4)));
    }

    @Test
    @DisplayName("Nhu cầu vắt qua năm (2026-W52 đến 2027-W02) kiểm tra active chính xác")
    void testCrossYearDemand_ActiveInCorrectWeeks() {
        ScenarioDemand demand = ScenarioDemand.create(
                1L, "Cross-Year Demand", 2, 2026, 52, 2027, 2, BigDecimal.valueOf(40), "Architect"
        );

        assertEquals(BigDecimal.valueOf(80), demand.getTotalHoursPerWeek());
        assertFalse(demand.isActiveInWeek(YearWeek.of(2026, 51)));
        assertTrue(demand.isActiveInWeek(YearWeek.of(2026, 52)));
        assertTrue(demand.isActiveInWeek(YearWeek.of(2027, 1)));
        assertTrue(demand.isActiveInWeek(YearWeek.of(2027, 2)));
        assertFalse(demand.isActiveInWeek(YearWeek.of(2027, 3)));
    }

    @Test
    @DisplayName("ResourceScenario: Kịch bản không ở trạng thái draft thì không thể sửa đổi")
    void testScenarioNotModifiableWhenNotDraft() {
        ResourceScenario scenario = ResourceScenario.createNew(
                "SCN-01", "Kịch bản 1", "Mô tả", 10L, 2026, 38, 8, 100L
        );
        scenario.setStatus(ScenarioStatus.APPLIED);

        assertThrows(ScenarioNotModifiableException.class, scenario::assertModifiable);

        scenario.setStatus(ScenarioStatus.DISCARDED);
        assertThrows(ScenarioNotModifiableException.class, scenario::assertModifiable);
    }
}
