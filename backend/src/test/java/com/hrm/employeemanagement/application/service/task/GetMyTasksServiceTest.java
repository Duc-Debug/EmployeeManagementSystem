package com.hrm.employeemanagement.application.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.MyTaskResult;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
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
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class GetMyTasksServiceTest {

    private static final Long USER_ID = 5L;
    private static final Long EMPLOYEE_ID = 10L;
    private static final Long TASK_ID = 100L;
    private static final Long PROJECT_ID = 200L;

    @Mock
    private GetAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadTaskAssignmentPort loadTaskAssignmentPort;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    private GetMyTasksService service;

    @BeforeEach
    void setUp() {
        service = new GetMyTasksService(
                authenticatedUserPort,
                loadEmployeePort,
                loadTaskAssignmentPort,
                loadTaskPort,
                loadProjectPort
        );
    }

    @Test
    @DisplayName("Lấy danh sách công việc được giao cho nhân sự đang đăng nhập thành công")
    void shouldReturnAssignedTasksForCurrentUser() {
        User user = new User(
                new UserId(USER_ID),
                "nv01",
                "hash",
                new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên"),
                UserStatus.ACTIVE,
                new EmployeeId(EMPLOYEE_ID),
                DataScope.SELF,
                null,
                1L);
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(user);

        Employee employee = new Employee(new EmployeeId(EMPLOYEE_ID), new UserId(USER_ID), 1L,
                "EMP10", "Nguyễn Văn A", false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(USER_ID))).thenReturn(Optional.of(employee));

        TaskAssignment assignment = new TaskAssignment(1L, new TaskId(TASK_ID), new EmployeeId(EMPLOYEE_ID),
                LocalDateTime.now(), new UserId(1L), true, LocalDateTime.now(), null, 1L);
        when(loadTaskAssignmentPort.findByEmployeeId(new EmployeeId(EMPLOYEE_ID))).thenReturn(List.of(assignment));

        Task task = new Task(new TaskId(TASK_ID), new ProjectId(PROJECT_ID), null, "PRJ-T01",
                "Thiết kế UI", null, TaskType.TASK, new EmployeeId(EMPLOYEE_ID), BigDecimal.valueOf(20),
                BigDecimal.ZERO, BigDecimal.ZERO, TaskStatus.IN_PROGRESS, 1, LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 25), new UserId(1L), LocalDateTime.now(), null, 1L);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        Project project = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án HRM",
                1L,
                new EmployeeId(1L),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                BigDecimal.ZERO,
                null,
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        List<MyTaskResult> results = service.getMyTasks();

        assertNotNull(results);
        assertEquals(1, results.size());
        MyTaskResult r = results.get(0);
        assertEquals(TASK_ID, r.taskId());
        assertEquals("PRJ-T01", r.taskCode());
        assertEquals("Thiết kế UI", r.taskName());
        assertEquals("Dự án HRM", r.projectName());
        assertEquals(TaskStatus.IN_PROGRESS, r.status());
        assertEquals(LocalDate.of(2026, 9, 15), r.plannedStartDate());
        assertEquals(LocalDate.of(2026, 9, 25), r.plannedEndDate());
    }
}
