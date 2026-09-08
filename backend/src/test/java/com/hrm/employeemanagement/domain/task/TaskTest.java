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

    @Test
    @DisplayName("Cập nhật thông tin chi tiết của Task thành công")
    void shouldUpdateTaskDetailsSuccessfully() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Tên cũ",
                "Mô tả cũ",
                TaskType.TASK,
                null,
                BigDecimal.ONE,
                1,
                new UserId(1L));

        task.updateDetails("Tên mới", "Mô tả mới", new BigDecimal("8.0"), 2);

        assertEquals("Tên mới", task.getName());
        assertEquals("Mô tả mới", task.getDescription());
        assertEquals(new BigDecimal("8.0"), task.getEstimatedHours());
        assertEquals(2, task.getSortOrder());
        assertNotNull(task.getUpdatedAt());
    }

    @Test
    @DisplayName("Cập nhật trạng thái và kiểm tra isCategory, isTask thành công")
    void shouldUpdateStatusAndCheckType() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Công việc",
                null,
                TaskType.TASK,
                null,
                BigDecimal.ONE,
                1,
                new UserId(1L));

        assertEquals(true, task.isTask());
        assertEquals(false, task.isCategory());

        task.updateStatus(TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());

        assertThrows(InvalidTaskDataException.class, () -> task.updateStatus(null));
    }

    @Test
    @DisplayName("Chặn khởi tạo task với sortOrder âm")
    void shouldThrowWhenSortOrderIsNegative() {
        assertThrows(InvalidTaskDataException.class, () -> Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Công việc",
                null,
                TaskType.TASK,
                null,
                BigDecimal.ONE,
                -1,
                new UserId(1L)));
    }

    @Test
    @DisplayName("Chặn cập nhật task với sortOrder âm")
    void shouldThrowWhenUpdatingWithNegativeSortOrder() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Công việc",
                null,
                TaskType.TASK,
                null,
                BigDecimal.ONE,
                1,
                new UserId(1L));

        assertThrows(InvalidTaskDataException.class, () -> task.updateDetails("Tên mới", "Mô tả", BigDecimal.ONE, -5));
    }

    @Test
    @DisplayName("Cập nhật với description null thì giữ nguyên mô tả cũ, chuỗi rỗng thì xóa về null")
    void testUpdateDetails_DescriptionSemantics() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Công việc",
                "Mô tả ban đầu",
                TaskType.TASK,
                null,
                BigDecimal.ONE,
                1,
                new UserId(1L));

        // description = null -> giữ nguyên mô tả cũ
        task.updateDetails("Tên đổi", null, null, null);
        assertEquals("Mô tả ban đầu", task.getDescription());
        assertEquals("Tên đổi", task.getName());

        // description = "" -> xóa về null
        task.updateDetails("Tên đổi", "   ", null, null);
        assertNull(task.getDescription());
    }

    @Test
    @DisplayName("Cập nhật với name null thì giữ nguyên tên cũ, name rỗng thì ném InvalidTaskDataException")
    void testUpdateDetails_NameSemantics() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Tên ban đầu",
                "Mô tả",
                TaskType.TASK,
                null,
                BigDecimal.ONE,
                1,
                new UserId(1L));

        // name = null -> giữ nguyên tên cũ
        task.updateDetails(null, "Mô tả mới", null, null);
        assertEquals("Tên ban đầu", task.getName());
        assertEquals("Mô tả mới", task.getDescription());

        // name = "" hoặc "   " -> ném lỗi
        assertThrows(InvalidTaskDataException.class, () -> task.updateDetails("   ", null, null, null));
    }

    @Test
    @DisplayName("Thiết lập ngân sách giờ công hợp lệ thành công")
    void shouldSetBudgetHoursSuccessfully() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Phân tích hệ thống",
                null,
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                1,
                new UserId(1L));

        assertEquals(BigDecimal.ZERO, task.getBudgetHours());
        assertEquals(TaskBudgetBurnStatus.NOT_SET, task.getBudgetBurnStatus());

        task.setBudgetHours(new BigDecimal("40.00"));
        assertEquals(new BigDecimal("40.00"), task.getBudgetHours());
        assertEquals(BigDecimal.ZERO.setScale(2), task.calculateBurnedPercentage());
        assertEquals(new BigDecimal("40.00"), task.getRemainingBudgetHours());
        assertEquals(false, task.isOverBudget());
        assertEquals(TaskBudgetBurnStatus.SAFE, task.getBudgetBurnStatus());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi đặt ngân sách giờ công âm")
    void shouldThrowWhenBudgetHoursIsNegative() {
        Task task = Task.createNew(
                new ProjectId(1L),
                null,
                "WBS-01",
                "Phân tích hệ thống",
                null,
                TaskType.TASK,
                null,
                BigDecimal.TEN,
                1,
                new UserId(1L));

        assertThrows(InvalidTaskDataException.class, () -> task.setBudgetHours(new BigDecimal("-5.0")));
    }

    @Test
    @DisplayName("Tính toán tỷ lệ đã dùng và phát hiện vượt ngân sách ăn mòn lợi nhuận chính xác")
    void shouldCalculateBurnedPercentageAndOverBudgetCorrectly() {
        // Task có actualHours = 35, budgetHours = 40 (dùng 87.5% -> WARNING)
        Task taskWarning = new Task(
                new TaskId(1L),
                new ProjectId(1L),
                null,
                "WBS-01",
                "Thiết kế UI",
                null,
                TaskType.TASK,
                null,
                new BigDecimal("40.00"),
                new BigDecimal("35.00"),
                new BigDecimal("40.00"),
                TaskStatus.IN_PROGRESS,
                1,
                new UserId(1L),
                null,
                null,
                1L);

        assertEquals(new BigDecimal("87.50"), taskWarning.calculateBurnedPercentage());
        assertEquals(TaskBudgetBurnStatus.WARNING, taskWarning.getBudgetBurnStatus());
        assertEquals(false, taskWarning.isOverBudget());
        assertEquals(new BigDecimal("5.00"), taskWarning.getRemainingBudgetHours());

        // Task có actualHours = 50, budgetHours = 40 (dùng 125% -> OVER_BUDGET)
        Task taskOver = new Task(
                new TaskId(2L),
                new ProjectId(1L),
                null,
                "WBS-02",
                "Lập trình Backend",
                null,
                TaskType.TASK,
                null,
                new BigDecimal("40.00"),
                new BigDecimal("50.00"),
                new BigDecimal("40.00"),
                TaskStatus.IN_PROGRESS,
                2,
                new UserId(1L),
                null,
                null,
                1L);

        assertEquals(new BigDecimal("125.00"), taskOver.calculateBurnedPercentage());
        assertEquals(TaskBudgetBurnStatus.OVER_BUDGET, taskOver.getBudgetBurnStatus());
        assertEquals(true, taskOver.isOverBudget());
        assertEquals(new BigDecimal("-10.00"), taskOver.getRemainingBudgetHours());
    }
}