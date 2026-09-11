package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.AssignTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskAssignmentResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.task.AssigneeInactiveException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AssignTaskServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long PROJECT_ID = 100L;
    private static final Long TASK_ID = 500L;
    private static final Long EMPLOYEE_ID_1 = 10L;
    private static final Long EMPLOYEE_ID_2 = 20L;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private LoadTaskAssignmentPort loadTaskAssignmentPort;

    @Mock
    private SaveTaskAssignmentPort saveTaskAssignmentPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private SaveProjectMemberPort saveProjectMemberPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    @Mock
    private AuthorizationService authorizationService;

    private AssignTaskService service;

    @BeforeEach
    void setUp() {
        service = new AssignTaskService(
                loadTaskPort,
                saveTaskPort,
                loadTaskAssignmentPort,
                saveTaskAssignmentPort,
                loadProjectPort,
                saveProjectMemberPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);

        org.mockito.Mockito.lenient().when(loadUserPort.findById(any()))
                .thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
    }

    private User createCompanyUser(Long userId) {
        return new User(
                new UserId(userId),
                "admin",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private User createSelfUser(Long userId) {
        return new User(
                new UserId(userId),
                "pm",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                null,
                DataScope.SELF,
                null,
                1L);
    }

    @Test
    @DisplayName("Giao việc thành công cho nhiều nhân sự kèm ngày mong muốn")
    void shouldAssignTaskToMultipleEmployeesSuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        Employee emp1 = createEmployee(EMPLOYEE_ID_1, EmployeeStatus.ACTIVE);
        Employee emp2 = createEmployee(EMPLOYEE_ID_2, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_1))).thenReturn(Optional.of(emp1));
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_2))).thenReturn(Optional.of(emp2));

        when(loadProjectPort.existsMember(PROJECT_ID, EMPLOYEE_ID_1)).thenReturn(true);
        when(loadProjectPort.existsMember(PROJECT_ID, EMPLOYEE_ID_2)).thenReturn(true);

        LocalDate startDate = LocalDate.of(2026, 9, 15);
        LocalDate endDate = LocalDate.of(2026, 9, 30);

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1, EMPLOYEE_ID_2),
                startDate,
                endDate
        );

        TaskAssignmentResult result = service.assignTask(command);

        assertNotNull(result);
        assertEquals(TASK_ID, result.taskId());
        assertEquals(2, result.assigneeIds().size());
        assertEquals(startDate, result.plannedStartDate());
        assertEquals(endDate, result.plannedEndDate());

        verify(saveTaskPort).save(any(Task.class));
        verify(saveTaskAssignmentPort).deleteByTaskIdAndEmployeeIdNotIn(any(), any());
    }

    @Test
    @DisplayName("Tự động thêm nhân sự vào dự án khi chưa phải thành viên")
    void shouldAutoEnrollEmployeeToProjectWhenNotMember() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        Employee emp1 = createEmployee(EMPLOYEE_ID_1, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_1))).thenReturn(Optional.of(emp1));

        when(loadProjectPort.existsMember(PROJECT_ID, EMPLOYEE_ID_1)).thenReturn(false);

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 30)
        );

        TaskAssignmentResult result = service.assignTask(command);

        assertNotNull(result);
        verify(saveProjectMemberPort).addMember(PROJECT_ID, EMPLOYEE_ID_1);
    }

    @Test
    @DisplayName("Cho phép PM tự giao việc cho chính mình kể cả khi chưa có trong bảng project_members")
    void shouldAllowAssigningToProjectManagerEvenIfNotInMembersTable() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Employee pmEmployee = createEmployee(CURRENT_USER_ID, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(CURRENT_USER_ID))).thenReturn(Optional.of(pmEmployee));

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(CURRENT_USER_ID),
                null,
                null
        );

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        TaskAssignmentResult result = service.assignTask(command);
        assertNotNull(result);
        assertEquals(List.of(CURRENT_USER_ID), result.assigneeIds());
    }

    @Test
    @DisplayName("Ném lỗi khi giao việc cho nhân sự không ở trạng thái ACTIVE")
    void shouldThrowWhenAssigneeIsNotActive() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        Employee inactiveEmp = createEmployee(EMPLOYEE_ID_1, EmployeeStatus.TERMINATED);
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_1))).thenReturn(Optional.of(inactiveEmp));

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1),
                null,
                null
        );

        assertThrows(AssigneeInactiveException.class, () -> service.assignTask(command));
    }

    @Test
    @DisplayName("Không được phép phân công nhân sự đã hết hạn hợp đồng trước ngày bắt đầu công việc")
    void shouldThrowWhenAssigneeContractExpiredBeforePlannedStartDate() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate contractEndDate = LocalDate.of(2026, 5, 31);
        Employee expiredEmp = new Employee(
                new EmployeeId(EMPLOYEE_ID_1),
                new UserId(EMPLOYEE_ID_1),
                1L,
                "EMP-1",
                "Nhân viên Hết Hạn",
                "Dev",
                LocalDate.of(2025, 1, 1),
                contractEndDate,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_1))).thenReturn(Optional.of(expiredEmp));

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1),
                startDate,
                LocalDate.of(2026, 6, 30)
        );

        AssigneeInactiveException ex = assertThrows(AssigneeInactiveException.class, () -> service.assignTask(command));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("đã kết thúc hợp đồng lao động"));
    }

    @Test
    @DisplayName("Không được phép phân công người cho hạng mục gom nhóm (CATEGORY)")
    void shouldThrowWhenAssigningToCategory() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Task category = createTask(TaskType.CATEGORY);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(category));

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1),
                null,
                null
        );

        assertThrows(InvalidTaskDataException.class, () -> service.assignTask(command));
    }

    @Test
    @DisplayName("Ném PermissionDeniedException và ghi audit log từ chối khi projectId nằm ngoài data scope của user")
    void shouldThrowPermissionDeniedWhenOutsideDataScope() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        User selfUser = createSelfUser(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(selfUser));

        // Dự án được quản lý bởi một PM khác (999L != CURRENT_USER_ID)
        Project project = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án",
                1L,
                new EmployeeId(999L),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                BigDecimal.ZERO,
                null,
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0
        );
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Employee currentEmp = createEmployee(CURRENT_USER_ID, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentEmp));

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1),
                null,
                null
        );

        assertThrows(PermissionDeniedException.class, () -> service.assignTask(command));
        verify(saveDeniedAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Cho phép phân công khi user có scope SELF và chính là PM quản lý dự án")
    void shouldAllowAssignTaskWhenWithinDataScope_Self() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        User selfUser = createSelfUser(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(selfUser));

        // Dự án do chính CURRENT_USER_ID quản lý
        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Employee currentEmp = createEmployee(CURRENT_USER_ID, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentEmp));

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        Employee emp1 = createEmployee(EMPLOYEE_ID_1, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_1))).thenReturn(Optional.of(emp1));
        when(loadProjectPort.existsMember(PROJECT_ID, EMPLOYEE_ID_1)).thenReturn(true);

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1),
                null,
                null
        );

        TaskAssignmentResult result = service.assignTask(command);
        assertNotNull(result);
        assertEquals(List.of(EMPLOYEE_ID_1), result.assigneeIds());
    }

    @Test
    @DisplayName("Loại bỏ trùng lặp employeeIds và gán is_primary đúng cho employee duy nhất đầu tiên")
    void shouldDeduplicateEmployeeIdsAndSetPrimaryProperly() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);

        Project project = createProject(ProjectStatus.ACTIVE);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));

        Task task = createTask(TaskType.TASK);
        when(loadTaskPort.findById(new TaskId(TASK_ID))).thenReturn(Optional.of(task));

        Employee emp1 = createEmployee(EMPLOYEE_ID_1, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(EMPLOYEE_ID_1))).thenReturn(Optional.of(emp1));
        when(loadProjectPort.existsMember(PROJECT_ID, EMPLOYEE_ID_1)).thenReturn(true);

        AssignTaskCommand command = new AssignTaskCommand(
                PROJECT_ID,
                TASK_ID,
                List.of(EMPLOYEE_ID_1, EMPLOYEE_ID_1),
                null,
                null
        );

        TaskAssignmentResult result = service.assignTask(command);

        assertNotNull(result);
        assertEquals(List.of(EMPLOYEE_ID_1), result.assigneeIds());
        verify(saveTaskPort).save(org.mockito.ArgumentMatchers.argThat(t ->
                t.getAssigneeId() != null && t.getAssigneeId().value().equals(EMPLOYEE_ID_1)
        ));
        verify(saveTaskAssignmentPort, org.mockito.Mockito.times(1)).save(
                org.mockito.ArgumentMatchers.argThat(a ->
                        a.getEmployeeId().value().equals(EMPLOYEE_ID_1) && a.isPrimary()
                )
        );
    }

    private Project createProject(ProjectStatus status) {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án Alpha",
                1L,
                new EmployeeId(CURRENT_USER_ID),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                BigDecimal.ZERO,
                null,
                status,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0
        );
    }

    private Task createTask(TaskType type) {
        return new Task(
                new TaskId(TASK_ID),
                new ProjectId(PROJECT_ID),
                null,
                "PRJ-01-T001",
                "Task 1",
                null,
                type,
                null,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                TaskStatus.TODO,
                1,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                1L
        );
    }

    private Employee createEmployee(Long id, EmployeeStatus status) {
        return new Employee(
                new EmployeeId(id),
                new UserId(id),
                1L,
                "EMP-" + id,
                "Nhân viên " + id,
                false,
                40,
                status
        );
    }
}
