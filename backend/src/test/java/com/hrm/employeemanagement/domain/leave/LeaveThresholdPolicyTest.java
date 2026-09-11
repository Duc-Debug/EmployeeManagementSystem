package com.hrm.employeemanagement.domain.leave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LeaveThresholdPolicy Domain Tests (NCL-05-CN-006 & TC-02)")
class LeaveThresholdPolicyTest {

    @Test
    @DisplayName("TC-02: 4 trên 5 người trong bộ phận cùng nghỉ một ngày -> Cảnh báo vượt ngưỡng (80% >= 50%)")
    void tc02_fourOutOfFiveEmployeesOnLeave_exceedsThreshold() {
        int totalEmployees = 5;
        int onLeaveCount = 4;
        Double threshold = 0.50;

        boolean isWarning = LeaveThresholdPolicy.isWarningExceeded(totalEmployees, onLeaveCount, threshold);
        assertTrue(isWarning, "4/5 người nghỉ phải kích hoạt cảnh báo");

        String msg = LeaveThresholdPolicy.buildWarningMessage(LocalDate.of(2026, 9, 15), totalEmployees, onLeaveCount, threshold);
        assertNotNull(msg);
        assertTrue(msg.contains("4/5"));
        assertTrue(msg.contains("80.0%"));
        assertTrue(msg.contains("50.0%"));
    }

    @Test
    @DisplayName("Số người nghỉ dưới ngưỡng quy định -> Không có cảnh báo (1 trên 5 người nghỉ = 20% < 50%)")
    void oneOutOfFiveEmployeesOnLeave_noWarning() {
        int totalEmployees = 5;
        int onLeaveCount = 1;
        Double threshold = 0.50;

        boolean isWarning = LeaveThresholdPolicy.isWarningExceeded(totalEmployees, onLeaveCount, threshold);
        assertFalse(isWarning, "1/5 người nghỉ không vượt ngưỡng 50%");

        String msg = LeaveThresholdPolicy.buildWarningMessage(LocalDate.of(2026, 9, 15), totalEmployees, onLeaveCount, threshold);
        assertNull(msg);
    }

    @Test
    @DisplayName("Trường hợp biên: Số người nghỉ bằng đúng ngưỡng (2 trên 4 người = 50% >= 50%) -> Cảnh báo")
    void exactThreshold_exceedsThreshold() {
        int totalEmployees = 4;
        int onLeaveCount = 2;
        Double threshold = 0.50;

        boolean isWarning = LeaveThresholdPolicy.isWarningExceeded(totalEmployees, onLeaveCount, threshold);
        assertTrue(isWarning, "Bằng đúng 50% phải kích hoạt cảnh báo");
    }

    @Test
    @DisplayName("Bộ phận không có ai hoặc không ai nghỉ -> Không cảnh báo")
    void zeroEmployeesOrZeroLeaves_noWarning() {
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(0, 0, 0.50));
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(10, 0, 0.50));
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(0, 2, 0.50));
    }
}
