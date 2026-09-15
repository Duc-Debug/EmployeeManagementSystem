package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.scenario.InvalidScenarioDemandException;
import com.hrm.employeemanagement.domain.exception.scenario.ScenarioNotModifiableException;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioDemandTest {

    @Test
    @DisplayName("Validation: headcount <= 0 ném InvalidScenarioDemandException")
    void testHeadcountLessThanOrEqualZero_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 0, 1, 4, BigDecimal.valueOf(40), "Java")
        );

        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", -2, 1, 4, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Validation: hoursPerWeekPerPerson < 0 ném InvalidScenarioDemandException")
    void testNegativeHours_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 1, 4, BigDecimal.valueOf(-5), "Java")
        );
    }

    @Test
    @DisplayName("Validation: weekStart > weekEnd ném InvalidScenarioDemandException")
    void testWeekStartGreaterThanWeekEnd_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 8, 4, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Validation: Tuần ngoài khoảng 1..53 ném InvalidScenarioDemandException")
    void testWeekOutOfRange_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 0, 4, BigDecimal.valueOf(40), "Java")
        );

        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "Demand 1", 2, 1, 54, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Validation: Tên nhu cầu rỗng ném InvalidScenarioDemandException")
    void testBlankDemandName_ThrowsException() {
        assertThrows(InvalidScenarioDemandException.class, () ->
                ScenarioDemand.create(1L, "   ", 2, 1, 4, BigDecimal.valueOf(40), "Java")
        );
    }

    @Test
    @DisplayName("Nhu cầu hợp lệ: Tính toán tổng giờ/tuần đúng (headcount * hours)")
    void testValidDemand_CalculatesTotalHoursCorrectly() {
        ScenarioDemand demand = ScenarioDemand.create(
                1L, "Tuyển Java Dev", 3, 2, 6, BigDecimal.valueOf(40), "Java Spring"
        );

        assertEquals(BigDecimal.valueOf(120), demand.getTotalHoursPerWeek());
        assertTrue(demand.isActiveInWeek(2));
        assertTrue(demand.isActiveInWeek(4));
        assertTrue(demand.isActiveInWeek(6));
        assertFalse(demand.isActiveInWeek(1));
        assertFalse(demand.isActiveInWeek(7));
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
