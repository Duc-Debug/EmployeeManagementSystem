package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.timesheet.AssignedTaskOptionResult;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class GetMyAssignedTasksForWorkLogServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
    @Mock
    private LoadTaskAssignmentPort loadTaskAssignmentPort;
    @Mock
    private AuthorizationService authorizationService;

    private GetMyAssignedTasksForWorkLogService service;

    private final Long userId = 100L;
    private final EmployeeId employeeId = new EmployeeId(10L);
    private final ProjectId projectId = new ProjectId(1L);

    @BeforeEach
    void setUp() {
        service = new GetMyAssignedTasksForWorkLogService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                authorizationService
        );
    }

    private Employee createMockEmployee() {
        return new Employee(
                employeeId,
                new UserId(userId),
                1L,
                "EMP-001",
                "Nguyễn Văn Chuyên Môn",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private Project createMockProject(ProjectStatus status) {
        return new Project(
                projectId,
                "PROJ-001",
                "Hệ Thống Quản Lý Nhân Sự",
                1L,
                null,
                null,
                null,
                null,
                null,
                status,
                null,
                null,
                null,
                0L
        );
    }

    private Task createMockTask(Long id, String code, TaskType type, EmployeeId assigneeId) {
        return new Task(
                new TaskId(id),
                projectId,
                null,
                code,
                "Task " + code,
                null,
                type,
                assigneeId,
                BigDecimal.valueOf(40),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0L
        );
    }

    @Test
    @DisplayName("HIGH 3: Phải lấy cả task được gán trực tiếp qua assigneeId và task từ TaskAssignment")
    void testGetMyAssignedTasks_IncludesDirectAssigneeAndTaskAssignment() {
        when(authorizationService.require(PermissionCode.WORK_LOG_READ)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));

        // Task 1 assigned directly via assigneeId
        Task directTask = createMockTask(101L, "TASK-101", TaskType.TASK, employeeId);
        when(loadTaskPort.findByAssigneeId(employeeId)).thenReturn(List.of(directTask));

        // Task 2 assigned via TaskAssignment
        TaskAssignment assignment = new TaskAssignment(
                1L,
                new TaskId(102L),
                employeeId,
                LocalDateTime.now(),
                new UserId(userId),
                true,
                LocalDateTime.now(),
                null,
                0L
        );
        when(loadTaskAssignmentPort.findByEmployeeId(employeeId)).thenReturn(List.of(assignment));

        Task assignedTask = createMockTask(102L, "TASK-102", TaskType.TASK, null);
        when(loadTaskPort.findAllById(List.of(new TaskId(102L)))).thenReturn(List.of(assignedTask));

        when(loadProjectPort.findAllById(List.of(projectId))).thenReturn(List.of(createMockProject(ProjectStatus.ACTIVE)));

        List<AssignedTaskOptionResult> results = service.getMyAssignedTasksForWorkLog();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.taskId().equals(101L)));
        assertTrue(results.stream().anyMatch(r -> r.taskId().equals(102L)));
    }
}