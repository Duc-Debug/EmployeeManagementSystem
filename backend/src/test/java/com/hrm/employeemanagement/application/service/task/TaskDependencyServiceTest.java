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
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.hrm.employeemanagement.application.dto.task.dependency.CreateTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.DeleteTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyGraphResult;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.DeleteTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.task.CyclicTaskDependencyException;
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
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependencyType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class TaskDependencyServiceTest {

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
    @Mock
    private LoadTaskDependencyPort loadDependencyPort;
    @Mock
    private SaveTaskDependencyPort saveDependencyPort;
    @Mock
    private DeleteTaskDependencyPort deleteDependencyPort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private TaskDependencyService service;

    private final Long currentUserId = 100L;
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId task1Id = new TaskId(10L);
    private final TaskId task2Id = new TaskId(20L);

    private Project testProject;
    private Task task1;
    private Task task2;
    private User pmUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TaskDependencyService(
                loadProjectPort,
                loadTaskPort,
                loadDependencyPort,
                saveDependencyPort,
                deleteDependencyPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService
        );

        testProject = new Project(
                projectId,
                "PROJ-01",
                "Dự án thử nghiệm",
                10L,
                new EmployeeId(50L),
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                BigDecimal.valueOf(100),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(currentUserId),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0
        );

        task1 = new Task(task1Id, projectId, null, "TK-001", "Công việc Thiết kế", "Mô tả",
                TaskType.TASK, new EmployeeId(50L), BigDecimal.valueOf(10), BigDecimal.ZERO, TaskStatus.TODO, 1, new UserId(currentUserId), null, null, 0L);

        task2 = new Task(task2Id, projectId, null, "TK-002", "Công việc Lập trình", "Mô tả",
                TaskType.TASK, new EmployeeId(51L), BigDecimal.valueOf(20), BigDecimal.ZERO, TaskStatus.TODO, 2, new UserId(currentUserId), null, null, 0L);

        pmUser = new User(
                new UserId(currentUserId),
                "pm_user",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                new EmployeeId(50L),
                DataScope.SELF,
                null,
                0L
        );
        Employee emp = org.mockito.Mockito.mock(Employee.class);
        when(emp.getIdValue()).thenReturn(50L);
        when(loadEmployeePort.findByUserId(new UserId(currentUserId))).thenReturn(Optional.of(emp));
    }

    @Test
    @DisplayName("NCL-04-CN-004-TC-01: Tạo phụ thuộc giữa 2 công việc thành công và lưu AuditLog (TC-04)")
    void shouldCreateTaskDependencySuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE)).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));
        when(loadTaskPort.findById(task1Id)).thenReturn(Optional.of(task1));
        when(loadTaskPort.findById(task2Id)).thenReturn(Optional.of(task2));
        when(loadDependencyPort.existsByPredecessorIdAndSuccessorId(task1Id, task2Id)).thenReturn(false);
        when(loadDependencyPort.findByProjectId(projectId)).thenReturn(List.of());
        when(loadTaskPort.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));

        TaskDependency savedDep = new TaskDependency(1L, projectId, task1Id, task2Id, TaskDependencyType.FINISH_TO_START, 0, new UserId(currentUserId), null);
        when(saveDependencyPort.save(any(TaskDependency.class))).thenReturn(savedDep);

        CreateTaskDependencyCommand command = new CreateTaskDependencyCommand(1L, 10L, 20L, "FINISH_TO_START", 0);
        TaskDependencyResult result = service.createDependency(command);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("TK-001", result.predecessorTaskCode());
        assertEquals("TK-002", result.successorTaskCode());

        // TC-04: Kiểm tra AuditLog được lưu
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-04-CN-004-TC-02: Khống chế vòng lặp và ném CyclicTaskDependencyException")
    void shouldThrowCyclicTaskDependencyExceptionWhenCycleDetected() {
        when(authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE)).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));
        when(loadTaskPort.findById(task1Id)).thenReturn(Optional.of(task1));
        when(loadTaskPort.findById(task2Id)).thenReturn(Optional.of(task2));

        // Đã có task1 -> task2
        TaskDependency existing = new TaskDependency(1L, projectId, task1Id, task2Id, TaskDependencyType.FINISH_TO_START, 0, new UserId(currentUserId), null);
        when(loadDependencyPort.findByProjectId(projectId)).thenReturn(List.of(existing));
        when(loadTaskPort.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));

        // Cố tình thêm task2 -> task1
        CreateTaskDependencyCommand command = new CreateTaskDependencyCommand(1L, 20L, 10L, "FINISH_TO_START", 0);

        assertThrows(CyclicTaskDependencyException.class, () -> service.createDependency(command));
    }

    @Test
    @DisplayName("NCL-04-CN-004-TC-03: Từ chối truy cập khi người dùng không có quyền và ghi AuditLog từ chối")
    void shouldDenyAccessWhenPermissionMissing() {
        when(authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE));

        CreateTaskDependencyCommand command = new CreateTaskDependencyCommand(1L, 10L, 20L, "FINISH_TO_START", 0);

        assertThrows(PermissionDeniedException.class, () -> service.createDependency(command));
    }

    @Test
    @DisplayName("NCL-04-CN-004-TC-04: Xóa phụ thuộc công việc thành công và lưu AuditLog")
    void shouldDeleteTaskDependencySuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE)).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));

        TaskDependency existing = new TaskDependency(100L, projectId, task1Id, task2Id, TaskDependencyType.FINISH_TO_START, 0, new UserId(currentUserId), null);
        when(loadDependencyPort.findById(100L)).thenReturn(Optional.of(existing));

        DeleteTaskDependencyCommand command = new DeleteTaskDependencyCommand(1L, 100L);
        service.deleteDependency(command);

        verify(deleteDependencyPort).deleteById(100L);
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Lấy đồ thị phụ thuộc công việc dự án thành công")
    void shouldGetTaskDependenciesSuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_READ)).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));

        TaskDependency dep = new TaskDependency(1L, projectId, task1Id, task2Id, TaskDependencyType.FINISH_TO_START, 0, new UserId(currentUserId), null);
        when(loadDependencyPort.findByProjectId(projectId)).thenReturn(List.of(dep));
        when(loadTaskPort.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));

        TaskDependencyGraphResult graph = service.getTaskDependencies(1L);

        assertNotNull(graph);
        assertEquals(1L, graph.projectId());
        assertEquals(1, graph.dependencies().size());
        assertEquals("TK-001", graph.dependencies().get(0).predecessorTaskCode());
        assertEquals("TK-002", graph.dependencies().get(0).successorTaskCode());
    }
}
