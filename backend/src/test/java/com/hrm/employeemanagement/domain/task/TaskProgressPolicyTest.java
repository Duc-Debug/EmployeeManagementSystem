package com.hrm.employeemanagement.domain.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;

@DisplayName("TaskProgressPolicy Tests (NCL-04-CN-002)")
class TaskProgressPolicyTest {

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"})
    @DisplayName("Cho phép cập nhật các trạng thái hợp lệ trong Whitelist của nhân viên chuyên môn")
    void shouldAllowValidProgressStatuses(TaskStatus status) {
        assertDoesNotThrow(() -> TaskProgressPolicy.validateProgressStatus(status));
    }

    @Test
    @DisplayName("Chặn khi trạng thái là null")
    void shouldRejectNullStatus() {
        InvalidTaskDataException ex = assertThrows(InvalidTaskDataException.class,
                () -> TaskProgressPolicy.validateProgressStatus(null));
        assertEquals("Trạng thái tiến độ công việc không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("Chặn trạng thái không nằm trong Whitelist (như CANCELLED)")
    void shouldRejectCancelledStatusForSpecialist() {
        InvalidTaskDataException ex = assertThrows(InvalidTaskDataException.class,
                () -> TaskProgressPolicy.validateProgressStatus(TaskStatus.CANCELLED));
        assertEquals("Nhân viên chuyên môn chỉ được chuyển trạng thái qua: Chưa bắt đầu (TODO), Đang làm (IN_PROGRESS), Chờ duyệt (IN_REVIEW) hoặc Hoàn thành (DONE)", ex.getMessage());
    }

    @Test
    @DisplayName("Kiểm tra tập hợp Whitelist trạng thái")
    void shouldExposeAllowedProgressStatuses() {
        assertTrue(TaskProgressPolicy.getAllowedProgressStatuses().contains(TaskStatus.TODO));
        assertTrue(TaskProgressPolicy.getAllowedProgressStatuses().contains(TaskStatus.IN_PROGRESS));
        assertTrue(TaskProgressPolicy.getAllowedProgressStatuses().contains(TaskStatus.IN_REVIEW));
        assertTrue(TaskProgressPolicy.getAllowedProgressStatuses().contains(TaskStatus.DONE));
        assertEquals(4, TaskProgressPolicy.getAllowedProgressStatuses().size());
    }
}
