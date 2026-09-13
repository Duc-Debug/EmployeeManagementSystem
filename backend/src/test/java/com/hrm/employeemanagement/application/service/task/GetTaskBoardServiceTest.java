package com.hrm.employeemanagement.application.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.TaskBoardQuery;
import com.hrm.employeemanagement.application.dto.task.TaskBoardResult;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTaskBoardService Tests (NCL-04-CN-006)")
class GetTaskBoardServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long EMPLOYEE_ID = 10L;
    private static final Long OTHER_EMPLOYEE_ID = 20L;
    private static final Long PROJECT_ID = 100L;

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

    private GetTaskBoardService service;

    private User currentUser;
    private Employee currentEmployee;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new GetTaskBoardService(
                authenticatedUserPort,
                loadEmployeePort,
                loadTaskAssignmentPort,
                loadTaskPort,
                loadProjectPort
        );

        currentUser = new User(
                new UserId(USER_ID),
                "employee",
                "hash",
                new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE,
                new EmployeeId(EMPLOYEE_ID),
                DataScope.SELF,
                null,
                1L
        );

        currentEmployee = new Employee(
                new EmployeeId(EMPLOYEE_ID),
                new UserId(USER_ID),
                1L,
                "EMP01",
                "Tran Thi B",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        project = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Dự án Beta",
                1L,
                new EmployeeId(999L),
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(30),
                BigDecimal.valueOf(100),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L
        );
    }

    @Test
    @DisplayName("Lấy bảng công việc cá nhân mặc định (SELF scope) phân loại đúng các cột trạng thái")
    void testGetTaskBoard_SelfScope_DefaultLoadsOwnTasks() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUser);
        when(loadEmployeePort.findByUserId(currentUser.getId())).thenReturn(Optional.of(currentEmployee));

        TaskId task1Id = new TaskId(1L);
        TaskId task2Id = new TaskId(2L);
        TaskId task3Id = new TaskId(3L);

        TaskAssignment assign1 = TaskAssignment.create(task1Id, new EmployeeId(EMPLOYEE_ID), new UserId(USER_ID), true);
        TaskAssignment assign2 = TaskAssignment.create(task2Id, new EmployeeId(EMPLOYEE_ID), new UserId(USER_ID), true);
        TaskAssignment assign3 = TaskAssignment.create(task3Id, new EmployeeId(EMPLOYEE_ID), new UserId(USER_ID), true);

        when(loadTaskAssignmentPort.findByEmployeeId(new EmployeeId(EMPLOYEE_ID)))
                .thenReturn(List.of(assign1, assign2, assign3));

        Task task1 = createTask(1L, "T1", TaskStatus.TODO, TaskType.TASK);
        Task task2 = createTask(2L, "T2", TaskStatus.IN_PROGRESS, TaskType.TASK);
        Task task3 = createTask(3L, "T3", TaskStatus.IN_REVIEW, TaskType.TASK);

        when(loadTaskPort.findAllById(List.of(task1Id, task2Id, task3Id)))
                .thenReturn(List.of(task1, task2, task3));

        when(loadTaskAssignmentPort.findByTaskIdIn(List.of(task1Id, task2Id, task3Id)))
                .thenReturn(List.of(assign1, assign2, assign3));

        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(EMPLOYEE_ID))))
                .thenReturn(List.of(currentEmployee));

        when(loadProjectPort.findAllById(List.of(new ProjectId(PROJECT_ID))))
                .thenReturn(List.of(project));

        // When: Không truyền query (lấy công việc của tôi)
        TaskBoardResult result = service.getTaskBoard(null);

        // Then: Phân chia chính xác vào các cột
        assertNotNull(result);
        assertEquals(3, result.totalTasks());
        assertEquals(1, result.todoTasks().size());
        assertEquals(1, result.inProgressTasks().size());
        assertEquals(1, result.inReviewTasks().size());
        assertEquals(0, result.doneTasks().size());
        assertEquals(0, result.cancelledTasks().size());

        // Kiểm tra cờ canMove là true vì là công việc của chính mình
        assertTrue(result.todoTasks().get(0).canMove());
        assertTrue(result.inProgressTasks().get(0).canMove());
        assertTrue(result.inReviewTasks().get(0).canMove());
    }

    @Test
    @DisplayName("Loại trừ các task có taskType == CATEGORY khỏi bảng công việc Kanban")
    void testGetTaskBoard_ExcludesCategoryTasks() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUser);
        when(loadEmployeePort.findByUserId(currentUser.getId())).thenReturn(Optional.of(currentEmployee));

        TaskId task1Id = new TaskId(1L);
        TaskId categoryId = new TaskId(2L);

        Task task = createTask(1L, "T1", TaskStatus.TODO, TaskType.TASK);
        Task category = createTask(2L, "CAT1", TaskStatus.TODO, TaskType.CATEGORY);

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID)))
                .thenReturn(List.of(task, category));

        when(loadTaskAssignmentPort.findByTaskIdIn(List.of(task1Id)))
                .thenReturn(Collections.emptyList());
        when(loadEmployeePort.findAllByIdIn(Collections.emptyList()))
                .thenReturn(Collections.emptyList());
        when(loadProjectPort.findAllById(List.of(new ProjectId(PROJECT_ID))))
                .thenReturn(List.of(project));

        TaskBoardResult result = service.getTaskBoard(new TaskBoardQuery(PROJECT_ID, null));

        assertEquals(1, result.totalTasks());
        assertEquals(1, result.todoTasks().size());
        assertEquals("T1", result.todoTasks().get(0).name());
    }

    @Test
    @DisplayName("Lọc bảng công việc theo dự án (projectId) và gắn cờ canMove = false cho thẻ của người khác")
    void testGetTaskBoard_FilterByProject_CardsOfOthersCannotBeMoved() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUser);
        when(loadEmployeePort.findByUserId(currentUser.getId())).thenReturn(Optional.of(currentEmployee));

        TaskId myTaskId = new TaskId(1L);
        TaskId otherTaskId = new TaskId(2L);

        Task myTask = createTask(1L, "Việc của tôi", TaskStatus.IN_PROGRESS, TaskType.TASK);
        Task otherTask = createTask(2L, "Việc của người khác", TaskStatus.IN_PROGRESS, TaskType.TASK);

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID)))
                .thenReturn(List.of(myTask, otherTask));

        TaskAssignment myAssign = TaskAssignment.create(myTaskId, new EmployeeId(EMPLOYEE_ID), new UserId(USER_ID), true);
        TaskAssignment otherAssign = TaskAssignment.create(otherTaskId, new EmployeeId(OTHER_EMPLOYEE_ID), new UserId(99L), true);

        when(loadTaskAssignmentPort.findByTaskIdIn(List.of(myTaskId, otherTaskId)))
                .thenReturn(List.of(myAssign, otherAssign));

        Employee otherEmployee = new Employee(
                new EmployeeId(OTHER_EMPLOYEE_ID),
                new UserId(99L),
                1L,
                "EMP02",
                "Le Van C",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(EMPLOYEE_ID), new EmployeeId(OTHER_EMPLOYEE_ID))))
                .thenReturn(List.of(currentEmployee, otherEmployee));

        when(loadProjectPort.findAllById(List.of(new ProjectId(PROJECT_ID))))
                .thenReturn(List.of(project));

        TaskBoardResult result = service.getTaskBoard(new TaskBoardQuery(PROJECT_ID, null));

        assertEquals(2, result.totalTasks());
        assertEquals(2, result.inProgressTasks().size());

        // Thẻ của tôi: canMove = true
        assertTrue(result.inProgressTasks().stream()
                .filter(c -> c.taskId().equals(1L))
                .findFirst().get().canMove());

        // Thẻ của người khác: canMove = false (chuẩn bị cho TC-02)
        assertFalse(result.inProgressTasks().stream()
                .filter(c -> c.taskId().equals(2L))
                .findFirst().get().canMove());
    }

    @Test
    @DisplayName("Tối ưu Anti-N+1: Xác minh findByTaskIdIn và findAllByIdIn chỉ được gọi duy nhất 1 lần")
    void testGetTaskBoard_BatchLoading_VerifiesSingleQueries() {
        when(authenticatedUserPort.getAuthenticatedUser()).thenReturn(currentUser);
        when(loadEmployeePort.findByUserId(currentUser.getId())).thenReturn(Optional.of(currentEmployee));

        TaskId t1 = new TaskId(1L);
        TaskId t2 = new TaskId(2L);

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID)))
                .thenReturn(List.of(
                        createTask(1L, "T1", TaskStatus.DONE, TaskType.TASK),
                        createTask(2L, "T2", TaskStatus.CANCELLED, TaskType.TASK)
                ));

        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(Collections.emptyList());
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(Collections.emptyList());
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(project));

        service.getTaskBoard(new TaskBoardQuery(PROJECT_ID, null));

        verify(loadTaskAssignmentPort, times(1)).findByTaskIdIn(List.of(t1, t2));
        verify(loadEmployeePort, times(1)).findAllByIdIn(Collections.emptyList());
        verify(loadProjectPort, times(1)).findAllById(List.of(new ProjectId(PROJECT_ID)));
    }

    private Task createTask(Long id, String name, TaskStatus status, TaskType taskType) {
        return new Task(
                new TaskId(id),
                new ProjectId(PROJECT_ID),
                null,
                "TSK-" + id,
                name,
                "Mô tả",
                taskType,
                taskType == TaskType.CATEGORY ? null : new EmployeeId(EMPLOYEE_ID),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(15),
                status,
                1,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null,
                0,
                new UserId(USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );
    }
}
