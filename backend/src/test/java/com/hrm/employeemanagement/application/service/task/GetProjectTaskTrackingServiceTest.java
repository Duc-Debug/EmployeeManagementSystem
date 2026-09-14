package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.tracking.ProjectTaskTrackingResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingItemResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingQuery;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
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
@DisplayName("GetProjectTaskTrackingService Tests (NCL-04-CN-003)")
class GetProjectTaskTrackingServiceTest {

    private static final Long PROJECT_ID = 100L;
    private static final Long PM_USER_ID = 1L;
    private static final Long PM_EMPLOYEE_ID = 10L;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private LoadTaskAssignmentPort loadTaskAssignmentPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    private GetProjectTaskTrackingService service;

    private User pmUser;
    private Employee pmEmployee;
    private Project activeProject;

    @BeforeEach
    void setUp() {
        service = new GetProjectTaskTrackingService(
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                loadEmployeePort,
                loadUserPort,
                authorizationService,
                saveDeniedAuditLogPort
        );

        pmUser = new User(
                new UserId(PM_USER_ID),
                "pm_user",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                new EmployeeId(PM_EMPLOYEE_ID),
                DataScope.SELF,
                null,
                1L
        );

        pmEmployee = new Employee(
                new EmployeeId(PM_EMPLOYEE_ID),
                new UserId(PM_USER_ID),
                1L,
                "EMP001",
                "Trần Quản Lý",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        activeProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Hệ thống Quản lý Bán hàng",
                1L,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(60),
                BigDecimal.valueOf(500),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(PM_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );
    }

    @Test
    @DisplayName("NCL-04-CN-003-TC-01: Hiển thị đầy đủ 10 công việc ở các trạng thái kèm hạng mục, người phụ trách, ngân sách")
    void shouldReturnAllTenTasksWithCategoryAndAssigneesSuccessfully() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        // Tạo hạng mục cha (CATEGORY)
        Task category1 = new Task(
                new TaskId(1L), new ProjectId(PROJECT_ID), null, "CAT-01", "Giai đoạn Thiết kế",
                "Hạng mục thiết kế", TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS, 1, LocalDate.now().minusDays(10), LocalDate.now().plusDays(20),
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(20), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        // Tạo 10 công việc (TASK)
        List<Task> allTasks = new ArrayList<>();
        allTasks.add(category1);

        TaskStatus[] statuses = {
                TaskStatus.TODO, TaskStatus.TODO,
                TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS,
                TaskStatus.IN_REVIEW, TaskStatus.IN_REVIEW,
                TaskStatus.DONE, TaskStatus.DONE, TaskStatus.DONE
        };

        for (int i = 0; i < 10; i++) {
            Long taskId = (long) (i + 2);
            allTasks.add(new Task(
                    new TaskId(taskId), new ProjectId(PROJECT_ID), new TaskId(1L), "TSK-0" + (i + 1), "Công việc số " + (i + 1),
                    "Mô tả", TaskType.TASK, new EmployeeId(100L), BigDecimal.valueOf(10), BigDecimal.valueOf(5), BigDecimal.valueOf(10),
                    statuses[i], i + 1, LocalDate.now().minusDays(5), LocalDate.now().plusDays(10),
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(10), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
            ));
        }

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(allTasks);

        // Giả lập phân công cho các task
        Employee dev = new Employee(new EmployeeId(100L), new UserId(2L), 1L, "EMP100", "Lập trình viên A", false, 40, EmployeeStatus.ACTIVE);
        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(List.of(
                TaskAssignment.create(new TaskId(2L), new EmployeeId(100L), new UserId(1L), true)
        ));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(dev));

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertNotNull(result);
        assertEquals(PROJECT_ID, result.projectId());
        assertEquals("PRJ-001", result.projectCode());
        assertEquals("Hệ thống Quản lý Bán hàng", result.projectName());
        assertEquals(10, result.totalTasks());
        assertEquals(3, result.completedTasks());
        assertEquals(3, result.inProgressTasks());
        assertEquals(10, result.tasks().size());

        // Kiểm tra thông tin công việc đầu tiên
        TaskTrackingItemResult firstItem = result.tasks().get(0);
        assertEquals(1L, firstItem.categoryId());
        assertEquals("Giai đoạn Thiết kế", firstItem.categoryName());
        assertNotNull(firstItem.plannedStartDate());
        assertNotNull(firstItem.plannedEndDate());
        assertNotNull(firstItem.budgetHours());
        assertNotNull(firstItem.actualHours());
    }

    @Test
    @DisplayName("NCL-04-CN-003-TC-02: Công việc quá hạn được đánh dấu isOverdue = true và ưu tiên sắp xếp lên ĐẦU danh sách")
    void shouldMarkOverdueTasksAndSortThemToTop() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        // Task 1: Chưa quá hạn (hạn chót sau 5 ngày)
        Task normalTask = new Task(
                new TaskId(10L), new ProjectId(PROJECT_ID), null, "TSK-10", "Việc bình thường",
                "Mô tả", TaskType.TASK, null, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.valueOf(10),
                TaskStatus.IN_PROGRESS, 1, LocalDate.now().minusDays(5), LocalDate.now().plusDays(5),
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        // Task 2: Quá hạn 3 ngày (plannedEndDate là 3 ngày trước, chưa DONE)
        Task overdueTask = new Task(
                new TaskId(20L), new ProjectId(PROJECT_ID), null, "TSK-20", "Việc bị trễ hạn",
                "Mô tả trễ", TaskType.TASK, null, BigDecimal.valueOf(10), BigDecimal.valueOf(12), BigDecimal.valueOf(10),
                TaskStatus.IN_PROGRESS, 2, LocalDate.now().minusDays(10), LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(normalTask, overdueTask));
        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(Collections.emptyList());

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertEquals(2, result.totalTasks());
        assertEquals(1, result.overdueTasks());

        // Xác nhận công việc trễ hạn được đẩy lên VỊ TRÍ ĐẦU TIÊN (index 0)
        TaskTrackingItemResult topItem = result.tasks().get(0);
        assertEquals(20L, topItem.taskId());
        assertEquals("TSK-20", topItem.taskCode());
        assertTrue(topItem.isOverdue());
        assertEquals(3L, topItem.overdueDays());

        // Công việc thứ hai là việc bình thường
        TaskTrackingItemResult secondItem = result.tasks().get(1);
        assertEquals(10L, secondItem.taskId());
        assertFalse(secondItem.isOverdue());
        assertEquals(0L, secondItem.overdueDays());
    }

    @Test
    @DisplayName("NCL-04-CN-003-TC-03: Dự án chưa có công việc nào -> Trả về danh sách rỗng kèm gợi ý tạo cây WBS")
    void shouldReturnEmptyResultWithSuggestionWhenNoTasksExist() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(Collections.emptyList());

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertEquals(0, result.totalTasks());
        assertEquals(0, result.overdueTasks());
        assertEquals(0, result.completedTasks());
        assertTrue(result.tasks().isEmpty());
        assertEquals("Dự án chưa có công việc nào. Gợi ý: Hãy tạo cây công việc (WBS) để bắt đầu theo dõi tiến độ.", result.suggestionMessage());
    }

    @Test
    @DisplayName("NCL-04-CN-003-TC-04: Người dùng không có quyền quản lý/truy cập dự án -> Ném PermissionDeniedException & Ghi AuditLog")
    void shouldRejectAndRecordAuditLogWhenUserHasNoAccess() {
        Long otherUserId = 99L;
        Long otherEmployeeId = 999L;

        User unauthorizedUser = new User(
                new UserId(otherUserId),
                "dev_b",
                "hash",
                new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE,
                new EmployeeId(otherEmployeeId),
                DataScope.SELF,
                null,
                1L
        );

        Employee otherEmployee = new Employee(
                new EmployeeId(otherEmployeeId),
                new UserId(otherUserId),
                1L,
                "EMP999",
                "Nguyễn Văn B",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(otherUserId);
        when(loadUserPort.findById(new UserId(otherUserId))).thenReturn(Optional.of(unauthorizedUser));
        when(loadEmployeePort.findByUserId(new UserId(otherUserId))).thenReturn(Optional.of(otherEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));
        when(loadProjectPort.existsMember(PROJECT_ID, otherEmployeeId)).thenReturn(false);

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);

        assertThrows(PermissionDeniedException.class, () -> service.getTaskTracking(query));
        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Quy tắc QTN-04: Dự án đã đóng (CLOSED) -> Từ chối thao tác và ném ProjectClosedException")
    void shouldRejectWhenProjectIsClosed() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));

        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Hệ thống Quản lý Bán hàng",
                1L,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.now().minusDays(60),
                LocalDate.now().minusDays(10),
                BigDecimal.valueOf(500),
                "Mô tả dự án",
                ProjectStatus.CLOSED,
                new UserId(PM_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );

        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);

        assertThrows(ProjectClosedException.class, () -> service.getTaskTracking(query));
    }

    @Test
    @DisplayName("Bộ lọc: Lọc chính xác theo người phụ trách (employeeId) và theo trạng thái (status)")
    void shouldFilterTasksByEmployeeIdAndStatus() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        Task taskA = new Task(
                new TaskId(1L), new ProjectId(PROJECT_ID), null, "TSK-01", "Task cho Dev A",
                "Mô tả", TaskType.TASK, new EmployeeId(101L), BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.valueOf(10),
                TaskStatus.IN_PROGRESS, 1, LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now(), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        Task taskB = new Task(
                new TaskId(2L), new ProjectId(PROJECT_ID), null, "TSK-02", "Task cho Dev B",
                "Mô tả", TaskType.TASK, new EmployeeId(102L), BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.valueOf(10),
                TaskStatus.TODO, 2, LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now(), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(taskA, taskB));
        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(List.of(
                TaskAssignment.create(new TaskId(1L), new EmployeeId(101L), new UserId(1L), true),
                TaskAssignment.create(new TaskId(2L), new EmployeeId(102L), new UserId(1L), true)
        ));

        // Lọc theo employeeId = 101L và status = IN_PROGRESS
        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, 101L, TaskStatus.IN_PROGRESS, null, null);
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertEquals(1, result.totalTasks());
        assertEquals("TSK-01", result.tasks().get(0).taskCode());
    }

    @Test
    @DisplayName("Cải tiến 3: Tra cứu Hạng mục tổ tiên khi công việc lồng dưới công việc cha (Ancestor Category Resolution)")
    void shouldResolveAncestorCategoryWhenTaskIsNestedUnderAnotherTask() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        // Cấp 1: Hạng mục cha
        Task rootCategory = new Task(
                new TaskId(100L), new ProjectId(PROJECT_ID), null, "CAT-01", "Giai đoạn Backend",
                "Mô tả", TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS, 1, LocalDate.now(), LocalDate.now().plusDays(30),
                LocalDate.now(), LocalDate.now().plusDays(30), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        // Cấp 2: Công việc cha (TASK) thuộc Hạng mục
        Task parentTask = new Task(
                new TaskId(101L), new ProjectId(PROJECT_ID), new TaskId(100L), "TSK-PARENT", "Xây dựng Module API",
                "Mô tả", TaskType.TASK, null, BigDecimal.valueOf(50), BigDecimal.ZERO, BigDecimal.valueOf(50),
                TaskStatus.IN_PROGRESS, 2, LocalDate.now(), LocalDate.now().plusDays(15),
                LocalDate.now(), LocalDate.now().plusDays(15), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        // Cấp 3: Công việc con (TASK) thuộc Công việc cha
        Task subTask = new Task(
                new TaskId(102L), new ProjectId(PROJECT_ID), new TaskId(101L), "TSK-CHILD", "Viết API Controller",
                "Mô tả", TaskType.TASK, null, BigDecimal.valueOf(20), BigDecimal.ZERO, BigDecimal.valueOf(20),
                TaskStatus.IN_PROGRESS, 3, LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now(), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(rootCategory, parentTask, subTask));
        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(Collections.emptyList());

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertEquals(2, result.totalTasks());

        // Công việc con (subTask) duyệt ngược lên tìm được đúng Hạng mục "Giai đoạn Backend"
        TaskTrackingItemResult childItem = result.tasks().stream()
                .filter(t -> t.taskId().equals(102L))
                .findFirst()
                .orElseThrow();
        assertEquals(100L, childItem.categoryId());
        assertEquals("Giai đoạn Backend", childItem.categoryName());
    }

    @Test
    @DisplayName("Cải tiến 2 & QTN-06: Đánh dấu isOverBudget = true khi giờ thực tế vượt ngân sách")
    void shouldFlagOverBudgetWhenActualHoursExceedBudgetHours() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        // Công việc có ngân sách 20h, thực tế 25h -> Vượt ngân sách
        Task overBudgetTask = new Task(
                new TaskId(55L), new ProjectId(PROJECT_ID), null, "TSK-55", "Tối ưu hóa Database",
                "Mô tả", TaskType.TASK, null, BigDecimal.valueOf(20), BigDecimal.valueOf(25), BigDecimal.valueOf(20),
                TaskStatus.IN_PROGRESS, 1, LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now(), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(overBudgetTask));
        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(Collections.emptyList());

        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, null);
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertEquals(1, result.totalTasks());
        TaskTrackingItemResult item = result.tasks().get(0);
        assertTrue(item.isOverBudget());
        assertEquals(com.hrm.employeemanagement.domain.task.TaskBudgetBurnStatus.OVER_BUDGET, item.budgetBurnStatus());
        assertEquals(new BigDecimal("125.00"), item.burnedPercentage());
    }

    @Test
    @DisplayName("Cải tiến 4: Bộ lọc tìm kiếm nhanh theo từ khóa (keyword) trên taskCode và taskName")
    void shouldFilterTasksByKeywordOnTaskCodeAndName() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(PM_USER_ID);
        when(loadUserPort.findById(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmUser));
        when(loadEmployeePort.findByUserId(new UserId(PM_USER_ID))).thenReturn(Optional.of(pmEmployee));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        Task task1 = new Task(
                new TaskId(1L), new ProjectId(PROJECT_ID), null, "TSK-LOGIN", "Xây dựng chức năng Login",
                "Mô tả", TaskType.TASK, null, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.valueOf(10),
                TaskStatus.IN_PROGRESS, 1, LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now(), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        Task task2 = new Task(
                new TaskId(2L), new ProjectId(PROJECT_ID), null, "TSK-REPORT", "Tạo Báo cáo Doanh thu",
                "Mô tả", TaskType.TASK, null, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.valueOf(10),
                TaskStatus.TODO, 2, LocalDate.now(), LocalDate.now().plusDays(5),
                LocalDate.now(), LocalDate.now().plusDays(5), null, 0, new UserId(1L), LocalDateTime.now(), null, 0L
        );

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(task1, task2));
        when(loadTaskAssignmentPort.findByTaskIdIn(any())).thenReturn(Collections.emptyList());

        // Tìm từ khóa "login" (không phân biệt hoa thường)
        TaskTrackingQuery query = new TaskTrackingQuery(PROJECT_ID, null, null, null, "login");
        ProjectTaskTrackingResult result = service.getTaskTracking(query);

        assertEquals(1, result.totalTasks());
        assertEquals("TSK-LOGIN", result.tasks().get(0).taskCode());
    }

    @Test
    @DisplayName("Ngoại lệ: Ném InvalidTaskDataException khi query hoặc projectId bị null")
    void shouldThrowWhenQueryOrProjectIdIsNull() {
        assertThrows(InvalidTaskDataException.class, () -> service.getTaskTracking(null));
        assertThrows(InvalidTaskDataException.class, () -> service.getTaskTracking(new TaskTrackingQuery(null, null, null, null, null)));
    }
}
