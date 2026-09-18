package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.task.TaskStatus;

class TaskDueReminderPolicyTest {

    private final LocalDate today = LocalDate.of(2026, 9, 18);

    @Test
    @DisplayName("isDueWithinDays: Trả về true khi hạn còn 1, 2, 3 ngày hoặc đúng hôm nay")
    void isDueWithinDays_validDays() {
        assertTrue(TaskDueReminderPolicy.isDueWithinDays(today, today, 3));
        assertTrue(TaskDueReminderPolicy.isDueWithinDays(today.plusDays(1), today, 3));
        assertTrue(TaskDueReminderPolicy.isDueWithinDays(today.plusDays(2), today, 3));
        assertTrue(TaskDueReminderPolicy.isDueWithinDays(today.plusDays(3), today, 3));
    }

    @Test
    @DisplayName("isDueWithinDays: Trả về false khi đã quá hạn hoặc còn hơn 3 ngày")
    void isDueWithinDays_outOfRange() {
        assertFalse(TaskDueReminderPolicy.isDueWithinDays(today.minusDays(1), today, 3));
        assertFalse(TaskDueReminderPolicy.isDueWithinDays(today.plusDays(4), today, 3));
        assertFalse(TaskDueReminderPolicy.isDueWithinDays(null, today, 3));
        assertFalse(TaskDueReminderPolicy.isDueWithinDays(today, null, 3));
    }

    @Test
    @DisplayName("isEligibleStatus: TC-02 Bỏ qua công việc đã DONE hoặc CANCELLED, chấp nhận TODO, IN_PROGRESS, IN_REVIEW")
    void isEligibleStatus_validation() {
        assertTrue(TaskDueReminderPolicy.isEligibleStatus(TaskStatus.TODO));
        assertTrue(TaskDueReminderPolicy.isEligibleStatus(TaskStatus.IN_PROGRESS));
        assertTrue(TaskDueReminderPolicy.isEligibleStatus(TaskStatus.IN_REVIEW));

        // TC-02: Hoàn thành không gửi nhắc
        assertFalse(TaskDueReminderPolicy.isEligibleStatus(TaskStatus.DONE));
        assertFalse(TaskDueReminderPolicy.isEligibleStatus(TaskStatus.CANCELLED));
        assertFalse(TaskDueReminderPolicy.isEligibleStatus(null));
    }

    @Test
    @DisplayName("buildDirectTaskUrl: Tạo đường dẫn trực tiếp mở công việc")
    void buildDirectTaskUrl_formatting() {
        assertEquals("/projects/10/tasks/101", TaskDueReminderPolicy.buildDirectTaskUrl(10L, 101L));
        assertEquals("/tasks/101", TaskDueReminderPolicy.buildDirectTaskUrl(null, 101L));
        assertEquals("/tasks", TaskDueReminderPolicy.buildDirectTaskUrl(null, null));
    }

    @Test
    @DisplayName("buildSourceEventKey: Tạo khóa sự kiện duy nhất theo QTN-19")
    void buildSourceEventKey_QTN19() {
        String key = TaskDueReminderPolicy.buildSourceEventKey(55L, LocalDate.of(2026, 9, 20));
        assertEquals("TASK_DUE_REMINDER:55:2026-09-20", key);
    }

    @Test
    @DisplayName("determineNotificationLevel: Gán mức CAO nếu còn <= 1 ngày, TRUNG_BINH nếu còn 2-3 ngày")
    void determineNotificationLevel_levels() {
        assertEquals(NotificationLevel.CAO, TaskDueReminderPolicy.determineNotificationLevel(0));
        assertEquals(NotificationLevel.CAO, TaskDueReminderPolicy.determineNotificationLevel(1));
        assertEquals(NotificationLevel.TRUNG_BINH, TaskDueReminderPolicy.determineNotificationLevel(2));
        assertEquals(NotificationLevel.TRUNG_BINH, TaskDueReminderPolicy.determineNotificationLevel(3));
    }
}
