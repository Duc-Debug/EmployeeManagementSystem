package com.hrm.employeemanagement.domain.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;

@DisplayName("TaskProgressPolicy Tests (NCL-04-CN-002)")
class TaskProgressPolicyTest {

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"})
    @DisplayName("Cho phép cập nhật các trạng thái hợp lệ của nhân viên chuyên môn")
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
    @DisplayName("Chặn nhân viên chuyên môn tự hủy công việc (CANCELLED)")
    void shouldRejectCancelledStatusForSpecialist() {
        InvalidTaskDataException ex = assertThrows(InvalidTaskDataException.class,
                () -> TaskProgressPolicy.validateProgressStatus(TaskStatus.CANCELLED));
        assertEquals("Nhân viên chuyên môn không được phép hủy công việc (CANCELLED)", ex.getMessage());
    }
}
