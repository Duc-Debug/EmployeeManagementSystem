package com.hrm.employeemanagement.domain.task;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("TaskBudgetPolicy - Kiểm tra chính sách đánh giá rủi ro ngân sách")
class TaskBudgetPolicyTest {

    private static final BigDecimal DEFAULT_BUDGET = new BigDecimal("40.00");

    @Test
    @DisplayName("Trả về NOT_SET khi ngân sách null hoặc bằng 0")
    void shouldReturnNotSet_WhenBudgetHoursNullOrZero() {
        assertEquals(TaskBudgetBurnStatus.NOT_SET, TaskBudgetPolicy.determineBurnStatus(null, new BigDecimal("50.00")));
        assertEquals(TaskBudgetBurnStatus.NOT_SET, TaskBudgetPolicy.determineBurnStatus(BigDecimal.ZERO, new BigDecimal("50.00")));
        assertEquals(TaskBudgetBurnStatus.NOT_SET, TaskBudgetPolicy.determineBurnStatus(new BigDecimal("-10.00"), new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("Trả về NOT_SET khi tỷ lệ phần trăm burned null")
    void shouldReturnNotSet_WhenBurnedPercentageNull() {
        assertEquals(TaskBudgetBurnStatus.NOT_SET, TaskBudgetPolicy.determineBurnStatus(DEFAULT_BUDGET, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "50.00", "79.99"})
    @DisplayName("Trả về SAFE khi tỷ lệ tiêu hao < 80%")
    void shouldReturnSafe_WhenBurnedPercentageBelowWarningThreshold(String percent) {
        TaskBudgetBurnStatus status = TaskBudgetPolicy.determineBurnStatus(DEFAULT_BUDGET, new BigDecimal(percent));
        assertEquals(TaskBudgetBurnStatus.SAFE, status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"80.00", "85.50", "99.99", "100.00"})
    @DisplayName("Trả về WARNING khi tỷ lệ tiêu hao từ 80% đến 100% (đạt trần ngân sách nhưng chưa vượt)")
    void shouldReturnWarning_WhenBurnedPercentageBetween80And100(String percent) {
        TaskBudgetBurnStatus status = TaskBudgetPolicy.determineBurnStatus(DEFAULT_BUDGET, new BigDecimal(percent));
        assertEquals(TaskBudgetBurnStatus.WARNING, status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.01", "105.00", "125.00", "250.00"})
    @DisplayName("Trả về OVER_BUDGET khi tỷ lệ tiêu hao > 100% (thực sự vượt ngân sách)")
    void shouldReturnOverBudget_WhenBurnedPercentageAbove100(String percent) {
        TaskBudgetBurnStatus status = TaskBudgetPolicy.determineBurnStatus(DEFAULT_BUDGET, new BigDecimal(percent));
        assertEquals(TaskBudgetBurnStatus.OVER_BUDGET, status);
    }

    @Test
    @DisplayName("Kiểm tra tính đồng nhất của isOverBudget với actualHours > budgetHours")
    void shouldBeConsistentWithIsOverBudget() {
        // Chưa vượt: actual <= budget
        assertEquals(false, TaskBudgetPolicy.isOverBudget(DEFAULT_BUDGET, new BigDecimal("39.99")));
        assertEquals(false, TaskBudgetPolicy.isOverBudget(DEFAULT_BUDGET, new BigDecimal("40.00")));

        // Vượt ngân sách: actual > budget
        assertEquals(true, TaskBudgetPolicy.isOverBudget(DEFAULT_BUDGET, new BigDecimal("40.01")));
        assertEquals(true, TaskBudgetPolicy.isOverBudget(DEFAULT_BUDGET, new BigDecimal("50.00")));
    }

    @Test
    @DisplayName("Kiểm tra giá trị hằng số chính sách nghiệp vụ")
    void shouldExposeCorrectThresholdConstants() {
        assertEquals(new BigDecimal("80.00"), TaskBudgetPolicy.WARNING_THRESHOLD_PERCENT);
        assertEquals(new BigDecimal("100.00"), TaskBudgetPolicy.OVER_BUDGET_THRESHOLD_PERCENT);
    }
}
