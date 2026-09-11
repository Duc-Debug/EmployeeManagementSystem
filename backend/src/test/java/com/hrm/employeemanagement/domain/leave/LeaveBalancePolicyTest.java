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
}
