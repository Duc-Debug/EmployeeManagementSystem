package com.hrm.employeemanagement.domain.leave;

import com.hrm.employeemanagement.domain.exception.leave.LeaveBalanceExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NCL-05-CN-005: Domain Policy Test cho Quỹ phép năm")
class LeaveBalancePolicyTest {

    @Test
    @DisplayName("TC-01: Tính đúng số ngày phép còn lại: Entitled (12) + Carried (2) - Approved (3) - Pending (2) = 9")
    void testCalculateRemainingDays_StandardFormula() {
        BigDecimal remaining = LeaveBalancePolicy.calculateRemainingDays(
                new BigDecimal("12.00"),
                new BigDecimal("2.00"),
                new BigDecimal("3.00"),
                new BigDecimal("2.00")
        );

        assertEquals(new BigDecimal("9.0"), remaining);
    }

    @Test
    @DisplayName("TC-01: Trường hợp không có phép chuyển tiếp và không có đơn pending: 12 + 0 - 4 - 0 = 8")
    void testCalculateRemainingDays_NoPendingAndNoCarryOver() {
        BigDecimal remaining = LeaveBalancePolicy.calculateRemainingDays(
                new BigDecimal("12.00"),
                BigDecimal.ZERO,
                new BigDecimal("4.00"),
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("8.0"), remaining);
    }

    @Test
    @DisplayName("TC-02: validateSufficientBalance thành công khi đủ phép, ném LeaveBalanceExceededException khi thiếu phép")
    void testValidateSufficientBalance() {
        BigDecimal remaining = new BigDecimal("2.0");

        // Đủ phép (yêu cầu 2 ngày <= 2.0 ngày còn lại) -> Không ném exception
        assertDoesNotThrow(() -> LeaveBalancePolicy.validateSufficientBalance(remaining, new BigDecimal("2.0")));

        // Thiếu phép (yêu cầu 3 ngày > 2.0 ngày còn lại) -> Ném exception
        LeaveBalanceExceededException ex = assertThrows(
                LeaveBalanceExceededException.class,
                () -> LeaveBalancePolicy.validateSufficientBalance(remaining, new BigDecimal("3.0"))
        );

        assertEquals(new BigDecimal("3.0"), ex.getRequestedDays());
        assertEquals(remaining, ex.getRemainingDays());
    }

    @Test
    @DisplayName("Review HIGH: Tính đúng số ngày làm việc thuộc từng năm cho đơn nghỉ vắt qua 2 năm (2026-12-30 -> 2027-01-05)")
    void testCalculateWorkingDaysInYear_CrossYear() {
        // 2026-12-30 (Thứ 4), 2026-12-31 (Thứ 5) -> 2 ngày làm việc thuộc năm 2026
        // 2027-01-01 (Thứ 6), 2027-01-02 (T7), 2027-01-03 (CN), 2027-01-04 (T2), 2027-01-05 (T3) -> 3 ngày làm việc thuộc năm 2027
        java.time.LocalDate startDate = java.time.LocalDate.of(2026, 12, 30);
        java.time.LocalDate endDate = java.time.LocalDate.of(2027, 1, 5);

        int daysIn2026 = LeaveBalancePolicy.calculateWorkingDaysInYear(startDate, endDate, 2026);
        int daysIn2027 = LeaveBalancePolicy.calculateWorkingDaysInYear(startDate, endDate, 2027);
        int daysIn2028 = LeaveBalancePolicy.calculateWorkingDaysInYear(startDate, endDate, 2028);

        assertEquals(2, daysIn2026, "Số ngày làm việc thuộc năm 2026 phải là 2 ngày (T4 30/12 và T5 31/12)");
        assertEquals(3, daysIn2027, "Số ngày làm việc thuộc năm 2027 phải là 3 ngày (T6 01/01, T2 04/01, T3 05/01)");
        assertEquals(0, daysIn2028, "Không thuộc năm 2028");
    }
}
