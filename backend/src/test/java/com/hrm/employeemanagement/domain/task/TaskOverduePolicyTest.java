package com.hrm.employeemanagement.domain.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.UserId;

@DisplayName("TaskOverduePolicy Tests (NCL-04-CN-003)")
class TaskOverduePolicyTest {

    private final LocalDate today = LocalDate.of(2026, 9, 14);

    private Task createTaskWithDates(LocalDate plannedStart, LocalDate plannedEnd, TaskStatus status) {
        return new Task(
                new TaskId(1L),
                new ProjectId(10L),
                null,
                "TSK-01",
                "Phát triển backend",
                "Mô tả công việc",
                TaskType.TASK,
                new EmployeeId(100L),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20),
                status,
                1,
                plannedStart,
                plannedEnd,
                plannedStart,
                plannedEnd,
                null,
                0,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );
    }

    @Test
    @DisplayName("Đánh dấu quá hạn khi plannedEndDate trước ngày hiện tại và trạng thái chưa hoàn thành")
    void shouldMarkAsOverdueWhenDeadlinePassedAndNotDone() {
        LocalDate fiveDaysAgo = today.minusDays(5);
        Task task = createTaskWithDates(today.minusDays(10), fiveDaysAgo, TaskStatus.IN_PROGRESS);

        assertTrue(TaskOverduePolicy.isOverdue(task, today));
        assertEquals(5L, TaskOverduePolicy.calculateOverdueDays(task, today));
    }

    @Test
    @DisplayName("Không đánh dấu quá hạn khi công việc đã hoàn thành (DONE) dù hạn chót trong quá khứ")
    void shouldNotMarkAsOverdueWhenTaskIsDone() {
        LocalDate fiveDaysAgo = today.minusDays(5);
        Task task = createTaskWithDates(today.minusDays(10), fiveDaysAgo, TaskStatus.DONE);

        assertFalse(TaskOverduePolicy.isOverdue(task, today));
        assertEquals(0L, TaskOverduePolicy.calculateOverdueDays(task, today));
    }

    @Test
    @DisplayName("Không đánh dấu quá hạn khi công việc bị hủy (CANCELLED)")
    void shouldNotMarkAsOverdueWhenTaskIsCancelled() {
        LocalDate fiveDaysAgo = today.minusDays(5);
        Task task = createTaskWithDates(today.minusDays(10), fiveDaysAgo, TaskStatus.CANCELLED);

        assertFalse(TaskOverduePolicy.isOverdue(task, today));
        assertEquals(0L, TaskOverduePolicy.calculateOverdueDays(task, today));
    }

    @Test
    @DisplayName("Không đánh dấu quá hạn khi plannedEndDate ở tương lai")
    void shouldNotMarkAsOverdueWhenDeadlineIsInFuture() {
        LocalDate inFiveDays = today.plusDays(5);
        Task task = createTaskWithDates(today, inFiveDays, TaskStatus.IN_PROGRESS);

        assertFalse(TaskOverduePolicy.isOverdue(task, today));
        assertEquals(0L, TaskOverduePolicy.calculateOverdueDays(task, today));
    }

    @Test
    @DisplayName("Không đánh dấu quá hạn khi plannedEndDate là ngày hôm nay")
    void shouldNotMarkAsOverdueWhenDeadlineIsToday() {
        Task task = createTaskWithDates(today.minusDays(2), today, TaskStatus.IN_PROGRESS);

        assertFalse(TaskOverduePolicy.isOverdue(task, today));
        assertEquals(0L, TaskOverduePolicy.calculateOverdueDays(task, today));
    }

    @Test
    @DisplayName("Xử lý an toàn khi task hoặc currentDate là null")
    void shouldHandleNullGracefully() {
        assertFalse(TaskOverduePolicy.isOverdue(null, today));
        assertEquals(0L, TaskOverduePolicy.calculateOverdueDays(null, today));

        Task task = createTaskWithDates(today.minusDays(5), today.minusDays(1), TaskStatus.TODO);
        assertFalse(TaskOverduePolicy.isOverdue(task, null));
        assertEquals(0L, TaskOverduePolicy.calculateOverdueDays(task, null));
    }
}
