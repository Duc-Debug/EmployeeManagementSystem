package com.hrm.employeemanagement.domain.task;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.UserId;

class TaskTest {

    @Test
    @DisplayName("Tạo mới Hạng mục (CATEGORY) hợp lệ thành công")
    void shouldCreateCategorySuccessfully() {
        Task task = Task.createNew(
                new ProjectId(1L), // <-- Đã cập nhật ProjectId
                null,
                "WBS-01",
                "Hạng mục khảo sát",
                "Mô tả hạng mục",
                TaskType.CATEGORY,
                null,
                new BigDecimal("40.0"),
                1,
                new UserId(1L));

        assertNotNull(task);
        assertEquals("Hạng mục khảo sát", task.getName());
        assertEquals(TaskType.CATEGORY, task.getTaskType());
        assertEquals(TaskStatus.TODO, task.getStatus());
        assertEquals(new BigDecimal("40.0"), task.getEstimatedHours());
        assertNull(task.getAssigneeId());
    }

    @Test
    @DisplayName("Tạo mới Công việc con (TASK) có người thực hiện thành công")
    void shouldCreateLeafTaskSuccessfully() {
        Task task = Task.createNew(
                new ProjectId(1L),
                new TaskId(10L),
                "WBS-01.01",
                "Khảo sát người dùng",
                "Chi tiết công việc",
                TaskType.TASK,
                new EmployeeId(5L),
                new BigDecimal("16.0"),
                1,
                new UserId(1L));

        assertNotNull(task);
        assertEquals("Khảo sát người dùng", task.getName());
        assertEquals(TaskType.TASK, task.getTaskType());
        assertEquals(new EmployeeId(5L), task.getAssigneeId());
        assertEquals(new TaskId(10L), task.getParentId());
    }

    @Test
    @DisplayName("Ném lỗi nếu tên Task bị rỗng hoặc khoảng trắng")
    void shouldThrowWhenNameIsBlank() {
        assertThrows(InvalidTaskDataException.class, () -> Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "   ",
                "Mô tả",
                TaskType.TASK,
                new EmployeeId(2L),
                BigDecimal.TEN,
                1,
                new UserId(1L)));
    }

    @Test
    @DisplayName("Ném lỗi nếu giờ dự kiến nhỏ hơn 0")
    void shouldThrowWhenEstimatedHoursNegative() {
        assertThrows(InvalidTaskDataException.class, () -> Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Viết code",
                "Mô tả",
                TaskType.TASK,
                new EmployeeId(2L),
                new BigDecimal("-5.0"),
                1,
                new UserId(1L)));
    }

    @Test
    @DisplayName("Ném lỗi nếu hạng mục CATEGORY được gán assignee trực tiếp khi tạo")
    void shouldThrowWhenCategoryHasAssignee() {
        assertThrows(InvalidTaskDataException.class, () -> Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Giai đoạn 1",
                "Mô tả",
                TaskType.CATEGORY,
                new EmployeeId(2L),
                BigDecimal.TEN,
                1,
                new UserId(1L)));
    }

    @Test
    @DisplayName("Ném lỗi khi task tự chọn chính mình làm cha (changeParent)")
    void shouldThrowWhenTaskSelectsItselfAsParent() {
        Task task = new Task(
                new TaskId(10L),
                new ProjectId(1L),
                null,
                "WBS-01",
                "Công việc",
                null,
                TaskType.TASK,
                new EmployeeId(2L),
                BigDecimal.TEN,
                BigDecimal.ZERO,
                TaskStatus.TODO,
                1,
                new UserId(1L),
                null,
                null,
                0L);

        assertThrows(InvalidTaskDataException.class, () -> task.changeParent(new TaskId(10L)));
    }

    @Test
    @DisplayName("Ném lỗi nếu projectId bị null")
    void shouldThrowWhenProjectIdIsNull() {
        assertThrows(InvalidTaskDataException.class, () -> Task.createNew(
                null,
                null,
                "WBS-01",
                "Công việc",
                null,
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                1,
                new UserId(1L)));
    }

    @Test
    @DisplayName("Ném lỗi khi gán người thực hiện cho Hạng mục qua method assignTo")
    void shouldThrowWhenAssigningCategoryToEmployee() {
        Task category = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Hạng mục lớn",
                null,
                TaskType.CATEGORY,
                null,
                BigDecimal.TEN,
                1,
                new UserId(1L));

        assertThrows(InvalidTaskDataException.class, () -> category.assignTo(new EmployeeId(5L)));
    }
}