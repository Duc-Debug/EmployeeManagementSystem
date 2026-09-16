package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ResourceScenarioJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioAllocationSnapshotJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.scenario.entity.ScenarioDemandJpaEntity;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioJpaEntityValidationTest {

    private void invokeLifecycleMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    @Test
    @DisplayName("ResourceScenarioJpaEntity: Năm 2025 chỉ có 52 tuần, fromWeek = 53 bị từ chối")
    void testResourceScenarioJpaEntity_Week53In52WeekYear_ThrowsException() {
        ResourceScenarioJpaEntity entity = new ResourceScenarioJpaEntity(
                null, "SCN-2025-53", "Test 53 weeks", "Desc", 1L, "draft",
                2025, 53, 8, LocalDateTime.now(), 1L, LocalDateTime.now(), null, 0L
        );

        Exception ex = assertThrows(Exception.class, () -> invokeLifecycleMethod(entity, "validateYearWeek"));
        assertTrue(ex.getCause() instanceof InvalidWeekNumberException,
                "Phải ném InvalidWeekNumberException khi tuần 53 không tồn tại trong năm");
    }

    @Test
    @DisplayName("ResourceScenarioJpaEntity: Năm 2020 có 53 tuần, fromWeek = 53 hợp lệ")
    void testResourceScenarioJpaEntity_Week53In53WeekYear_Succeeds() {
        ResourceScenarioJpaEntity entity = new ResourceScenarioJpaEntity(
                null, "SCN-2020-53", "Test 53 weeks", "Desc", 1L, "draft",
                2020, 53, 8, LocalDateTime.now(), 1L, LocalDateTime.now(), null, 0L
        );

        assertDoesNotThrow(() -> invokeLifecycleMethod(entity, "validateYearWeek"));
    }

    @Test
    @DisplayName("ScenarioDemandJpaEntity: Tuần 53 trong năm 52 tuần bị từ chối ở cả startWeek và endWeek")
    void testScenarioDemandJpaEntity_Week53In52WeekYear_ThrowsException() {
        ScenarioDemandJpaEntity invalidStart = new ScenarioDemandJpaEntity(
                null, 1L, "Dev", 2, 2025, 53, 2026, 2, BigDecimal.valueOf(40), "Java",
                LocalDateTime.now(), null
        );
        Exception ex1 = assertThrows(Exception.class, () -> invokeLifecycleMethod(invalidStart, "validateYearWeeks"));
        assertTrue(ex1.getCause() instanceof InvalidWeekNumberException);

        ScenarioDemandJpaEntity invalidEnd = new ScenarioDemandJpaEntity(
                null, 1L, "Dev", 2, 2025, 50, 2027, 53, BigDecimal.valueOf(40), "Java",
                LocalDateTime.now(), null
        );
        Exception ex2 = assertThrows(Exception.class, () -> invokeLifecycleMethod(invalidEnd, "validateYearWeeks"));
        assertTrue(ex2.getCause() instanceof InvalidWeekNumberException);
    }

    @Test
    @DisplayName("ScenarioAllocationSnapshotJpaEntity: Tuần 53 trong năm 52 tuần bị từ chối ở tầng snapshot persistence")
    void testScenarioAllocationSnapshotJpaEntity_Week53In52WeekYear_ThrowsException() {
        ScenarioAllocationSnapshotJpaEntity invalidSnapshot = new ScenarioAllocationSnapshotJpaEntity(
                null, 1L, 100L, 2025, 53, BigDecimal.valueOf(20), BigDecimal.valueOf(40)
        );

        Exception ex = assertThrows(Exception.class, () -> invokeLifecycleMethod(invalidSnapshot, "validateYearWeek"));
        assertTrue(ex.getCause() instanceof InvalidWeekNumberException);
    }
}
