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
        assertTrue(msg.contains("đạt hoặc vượt ngưỡng quy định"));
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
    @DisplayName("Boundary test: 40% (< 50%), 50% (== 50%), 60% (> 50%) với ngưỡng 50%")
    void boundaryTests_atBelowAndAboveFiftyPercent() {
        int total = 10;
        Double threshold = 0.50;

        // 4/10 = 40% (dưới ngưỡng) -> Không cảnh báo
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(total, 4, threshold));
        assertNull(LeaveThresholdPolicy.buildWarningMessage(LocalDate.of(2026, 9, 15), total, 4, threshold));

        // 5/10 = 50% (đạt đúng ngưỡng) -> Kích hoạt cảnh báo
        assertTrue(LeaveThresholdPolicy.isWarningExceeded(total, 5, threshold));
        String msg50 = LeaveThresholdPolicy.buildWarningMessage(LocalDate.of(2026, 9, 15), total, 5, threshold);
        assertNotNull(msg50);
        assertTrue(msg50.contains("5/10"));
        assertTrue(msg50.contains("50.0%"));
        assertTrue(msg50.contains("đạt hoặc vượt ngưỡng quy định (50.0%)"));

        // 6/10 = 60% (vượt trên ngưỡng) -> Kích hoạt cảnh báo
        assertTrue(LeaveThresholdPolicy.isWarningExceeded(total, 6, threshold));
        String msg60 = LeaveThresholdPolicy.buildWarningMessage(LocalDate.of(2026, 9, 15), total, 6, threshold);
        assertNotNull(msg60);
        assertTrue(msg60.contains("6/10"));
        assertTrue(msg60.contains("60.0%"));
        assertTrue(msg60.contains("đạt hoặc vượt ngưỡng quy định (50.0%)"));
    }

    @Test
    @DisplayName("Bộ phận không có ai hoặc không ai nghỉ -> Không cảnh báo")
    void zeroEmployeesOrZeroLeaves_noWarning() {
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(0, 0, 0.50));
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(10, 0, 0.50));
        assertFalse(LeaveThresholdPolicy.isWarningExceeded(0, 2, 0.50));
    }

    @Test
    @DisplayName("Cải tiến P1: Ngày nghỉ cuối tuần hoặc ngày lễ (isCompanyWorkingDay = false) -> Bỏ qua cảnh báo dù 4/5 người nghỉ")
    void weekendOrHoliday_suppressesWarningEvenIfHighLeaveCount() {
        int totalEmployees = 5;
        int onLeaveCount = 4;
        Double threshold = 0.50;

        boolean isWarning = LeaveThresholdPolicy.isWarningExceeded(totalEmployees, onLeaveCount, threshold, false);
        assertFalse(isWarning, "Ngày không làm việc của công ty không được kích hoạt cảnh báo");

        String msg = LeaveThresholdPolicy.buildWarningMessage(LocalDate.of(2026, 9, 13), totalEmployees, onLeaveCount, threshold, false);
        assertNull(msg, "Thông điệp cảnh báo phải null khi không vượt ngưỡng do ngày nghỉ");
    }
}
