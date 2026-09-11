package com.hrm.employeemanagement.domain.task.dependency;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;

class TaskDependencyPolicyTest {

    private final ProjectId projectId = new ProjectId(1L);
    private final Map<Long, String> taskNames = Map.of(
            1L, "Thiết kế (TK-001)",
            2L, "Lập trình (TK-002)",
            3L, "Kiểm thử (TK-003)"
    );

    @Test
    @DisplayName("NCL-04-CN-004-TC-01: Cho phép thêm phụ thuộc hợp lệ không có vòng lặp")
    void shouldAllowValidDependencyWithoutCycle() {
        TaskId task1 = new TaskId(1L);
        TaskId task2 = new TaskId(2L);

        assertDoesNotThrow(() ->
                TaskDependencyPolicy.validateNoCycle(Collections.emptyList(), task1, task2, taskNames)
        );
    }

    @Test
    @DisplayName("NCL-04-CN-004-TC-02: Bắt lỗi khi khai báo phụ thuộc trực tiếp A -> B rồi B -> A")
    void shouldDetectDirectCycleBetweenTwoTasks() {
        TaskId task1 = new TaskId(1L);
        TaskId task2 = new TaskId(2L);

        // Đã có Task 1 -> Task 2
        TaskDependency existingDep = TaskDependency.createNew(
                projectId, task1, task2, TaskDependencyType.FINISH_TO_START, 0, null);

        // Cố tình thêm Task 2 -> Task 1
        CyclicTaskDependencyException ex = assertThrows(
                CyclicTaskDependencyException.class,
                () -> TaskDependencyPolicy.validateNoCycle(List.of(existingDep), task2, task1, taskNames)
        );

        assertEquals("Không thể tạo phụ thuộc: Phát hiện vòng lặp giữa các công việc [Lập trình (TK-002) -> Thiết kế (TK-001) -> Lập trình (TK-002)]", ex.getMessage());
    }

    @Test
    @DisplayName("NCL-04-CN-004-TC-02: Bắt lỗi khi khai báo phụ thuộc gián tiếp A -> B -> C rồi C -> A")
    void shouldDetectIndirectCycleThroughMultipleTasks() {
        TaskId task1 = new TaskId(1L);
        TaskId task2 = new TaskId(2L);
        TaskId task3 = new TaskId(3L);

        // Đã có 1 -> 2 và 2 -> 3
        TaskDependency dep1 = TaskDependency.createNew(projectId, task1, task2, TaskDependencyType.FINISH_TO_START, 0, null);
        TaskDependency dep2 = TaskDependency.createNew(projectId, task2, task3, TaskDependencyType.FINISH_TO_START, 0, null);

        // Cố tình thêm 3 -> 1
        CyclicTaskDependencyException ex = assertThrows(
                CyclicTaskDependencyException.class,
                () -> TaskDependencyPolicy.validateNoCycle(List.of(dep1, dep2), task3, task1, taskNames)
        );

        assertEquals("Không thể tạo phụ thuộc: Phát hiện vòng lặp giữa các công việc [Kiểm thử (TK-003) -> Thiết kế (TK-001) -> Lập trình (TK-002) -> Kiểm thử (TK-003)]", ex.getMessage());
    }
}
