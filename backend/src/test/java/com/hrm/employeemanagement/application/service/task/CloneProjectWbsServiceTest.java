package com.hrm.employeemanagement.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsCommand;
import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.CannotCloneFromSameProjectException;
import com.hrm.employeemanagement.domain.exception.task.EmptySourceWbsException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
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
class CloneProjectWbsServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long TARGET_PROJECT_ID = 100L;
    private static final Long SOURCE_PROJECT_ID = 200L;
    private static final Long MANAGER_ID = 10L;

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private SaveProjectPort saveProjectPort;

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

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private CloneProjectWbsService service;

    @BeforeEach
    void setUp() {
        service = new CloneProjectWbsService(
                loadTaskPort,
                saveTaskPort,
                loadProjectPort,
                saveProjectPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    // ==========================================
    // HELPER BUILDERS
    // ==========================================
    private User createCompanyUser(Long userId) {
        return new User(
                new UserId(userId),
                "pm_director",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "PM"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private User createSelfScopeUser(Long userId) {
        return new User(
                new UserId(userId),
                "pm_self",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "PM"),
                UserStatus.ACTIVE,
                null,
                DataScope.SELF,
                null,
                1L);
    }

    private Project createActiveProject(Long projectId, String code, Long managerId) {
        return new Project(
                new ProjectId(projectId),
                code,
                "Dự án " + code,
                10L,
                managerId != null ? new EmployeeId(managerId) : null,
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                new BigDecimal("100.00"),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);
    }

    /**
     * Tạo giả lập 12 tasks ở dự án nguồn: - 2 Hạng mục chính (Categories): ID
     * 1, 2 - 10 Công việc con (Tasks): ID 3..12 (mỗi hạng mục chứa 5 tasks)
     */
    private List<Task> createTwelveSourceTasks(Long sourceProjectId) {
        List<Task> tasks = new ArrayList<>();
        ProjectId prjId = new ProjectId(sourceProjectId);

        // 2 Categories
        Task cat1 = new Task(new TaskId(1L), prjId, null, "SRC-T001", "Giai đoạn Phân tích", "Mô tả",
                TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.TODO, 1, new UserId(99L), LocalDateTime.now(), null, 0L);
        Task cat2 = new Task(new TaskId(2L), prjId, null, "SRC-T002", "Giai đoạn Triển khai", "Mô tả",
                TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.TODO, 2, new UserId(99L), LocalDateTime.now(), null, 0L);
        tasks.add(cat1);
        tasks.add(cat2);

        // 10 Tasks con (đều đã được gán người phụ trách và có ngân sách giờ)
        for (int i = 3; i <= 7; i++) {
            tasks.add(new Task(new TaskId((long) i), prjId, new TaskId(1L), "SRC-T00" + i, "Công việc con A" + i,
                    "Chi tiết", TaskType.TASK, new EmployeeId(50L), new BigDecimal("10.0"),
                    new BigDecimal("5.0"), new BigDecimal("12.0"), TaskStatus.IN_PROGRESS, i,
                    new UserId(99L), LocalDateTime.now(), null, 0L));
        }
        for (int i = 8; i <= 12; i++) {
            tasks.add(new Task(new TaskId((long) i), prjId, new TaskId(2L), "SRC-T0" + i, "Công việc con B" + i,
                    "Chi tiết", TaskType.TASK, new EmployeeId(51L), new BigDecimal("20.0"),
                    new BigDecimal("18.5"), new BigDecimal("25.0"), TaskStatus.DONE, i,
                    new UserId(99L), LocalDateTime.now(), null, 0L));
        }

        return tasks;
    }

    // ==========================================
    // TEST CASES (NCL-03-CN-008-TC-01 -> TC-04)
    // ==========================================
    @Test
    @DisplayName("NCL-03-CN-008-TC-01: Luồng thành công - Sao chép đủ 12 công việc và không mang theo người phụ trách")
    void testCloneWbs_TC01_Success_12Tasks_NoAssignee() {
        // Given
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-OLD", MANAGER_ID);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));

        List<Task> sourceTasks = createTwelveSourceTasks(SOURCE_PROJECT_ID);
        when(loadTaskPort.findAllByProjectId(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(sourceTasks);

        // Mô phỏng gán ID tự tăng khi saveTaskPort lưu
        AtomicLong generatedId = new AtomicLong(1000L);
        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> {
            Task taskToSave = invocation.getArgument(0);
            return new Task(
                    new TaskId(generatedId.incrementAndGet()),
                    taskToSave.getProjectId(),
                    taskToSave.getParentId(),
                    taskToSave.getTaskCode(),
                    taskToSave.getName(),
                    taskToSave.getDescription(),
                    taskToSave.getTaskType(),
                    taskToSave.getAssigneeId(),
                    taskToSave.getEstimatedHours(),
                    taskToSave.getActualHours(),
                    taskToSave.getBudgetHours(),
                    taskToSave.getStatus(),
                    taskToSave.getSortOrder(),
                    taskToSave.getCreatedBy(),
                    LocalDateTime.now(),
                    null,
                    0L);
        });

        // When
        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);
        CloneProjectWbsResult result = service.cloneWbs(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.targetProjectId()).isEqualTo(TARGET_PROJECT_ID);
        assertThat(result.sourceProjectId()).isEqualTo(SOURCE_PROJECT_ID);
        assertThat(result.totalClonedTasks()).isEqualTo(12);
        assertThat(result.totalCategories()).isEqualTo(2);

        // Kiểm tra toàn bộ 12 tasks đã lưu
        verify(saveTaskPort, times(12)).save(taskCaptor.capture());
        List<Task> savedTasks = taskCaptor.getAllValues();

        for (Task task : savedTasks) {
            assertThat(task.getProjectId().value()).isEqualTo(TARGET_PROJECT_ID);
            // Quy tắc: chưa có người phụ trách
            assertThat(task.getAssigneeId()).isNull();
            // Quy tắc: trạng thái TODO
            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
            // Quy tắc: sinh mã theo prefix dự án đích
            assertThat(task.getTaskCode()).startsWith("PRJ-NEW-T");
        }

        // Kiểm tra sequence counter của target project đã tăng lên 12
        assertThat(targetProject.getTaskSeqCounter()).isEqualTo(12);
        verify(saveProjectPort).save(targetProject);
    }

    @Test
    @DisplayName("NCL-03-CN-008-TC-02: Ngoại lệ - Giữ nguyên ngân sách giờ công nhưng không mang theo giờ công thực tế")
    void testCloneWbs_TC02_SourceActualHours_NotCopied() {
        // Given
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-OLD", MANAGER_ID);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));

        // Task nguồn có giờ thực tế là 25.5h và ngân sách là 40.0h
        Task sourceTaskWithActual = new Task(
                new TaskId(5L),
                new ProjectId(SOURCE_PROJECT_ID),
                null,
                "SRC-T005",
                "Phát triển API Auth",
                "Mô tả công việc",
                TaskType.TASK,
                new EmployeeId(12L),
                new BigDecimal("30.0"),
                new BigDecimal("25.5"), // Đã phát sinh giờ công thực tế
                new BigDecimal("40.0"), // Ngân sách dự toán
                TaskStatus.IN_PROGRESS,
                1,
                new UserId(99L),
                LocalDateTime.now(),
                null,
                0L);

        when(loadTaskPort.findAllByProjectId(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(List.of(sourceTaskWithActual));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.cloneWbs(new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID));

        // Then
        verify(saveTaskPort).save(taskCaptor.capture());
        Task clonedTask = taskCaptor.getValue();

        assertThat(clonedTask.getBudgetHours()).isEqualByComparingTo(new BigDecimal("40.0"));
        // Ràng buộc TC-02: actualHours bắt buộc phải là 0
        assertThat(clonedTask.getActualHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(clonedTask.getAssigneeId()).isNull();
    }

    @Test
    @DisplayName("NCL-03-CN-008-TC-03: Không có quyền - Người dùng ngoài phạm vi quản lý bị từ chối và ghi nhật ký")
    void testCloneWbs_TC03_PermissionDenied_AuditRecorded() {
        // Given
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        // User có data scope SELF (chỉ quản lý dự án do mình làm PM)
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createSelfScopeUser(CURRENT_USER_ID)));

        // User này là Employee ID = 999L
        Employee currentEmployee = new Employee(
                new EmployeeId(999L),
                new UserId(CURRENT_USER_ID),
                10L,
                "EMP-999",
                "Nguyễn Văn PM",
                false,
                40,
                EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentEmployee));

        // Target project do Manager khác quản lý (MANAGER_ID = 10L != 999L)
        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));

        // When & Then
        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);
        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(PermissionDeniedException.class);

        // Kiểm tra NCL-03-CN-008-TC-03: Ghi nhận nhật ký lần từ chối truy cập
        verify(saveDeniedAuditLogPort).save(auditLogCaptor.capture());
        AuditLog deniedAudit = auditLogCaptor.getValue();

        assertThat(deniedAudit.getAction()).isEqualTo("PROJECT_ACCESS_DENIED");
        assertThat(deniedAudit.getTableName()).isEqualTo("projects");
        assertThat(deniedAudit.getRecordId()).isEqualTo(TARGET_PROJECT_ID);
        assertThat(deniedAudit.getNewValue()).contains("OUTSIDE_DATA_SCOPE_WBS_CLONE");

        // Không thực hiện bất kỳ hành động sao chép task nào
        verify(saveTaskPort, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("NCL-03-CN-008-TC-04: Lưu lịch sử - Ghi nhật ký CLONE_PROJECT_WBS khi hoàn tất thành công")
    void testCloneWbs_TC04_AuditLogged_OnSuccess() {
        // Given
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-OLD", MANAGER_ID);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));

        Task singleTask = new Task(new TaskId(1L), new ProjectId(SOURCE_PROJECT_ID), null, "SRC-T001",
                "Hạng mục duy nhất", "Mô tả", TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, TaskStatus.TODO, 1, new UserId(CURRENT_USER_ID), LocalDateTime.now(), null, 0L);

        when(loadTaskPort.findAllByProjectId(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(List.of(singleTask));
        when(saveTaskPort.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.cloneWbs(new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID));

        // Then
        verify(saveAuditLogPort).save(auditLogCaptor.capture());
        AuditLog successAudit = auditLogCaptor.getValue();

        assertThat(successAudit.getAction()).isEqualTo("CLONE_PROJECT_WBS");
        assertThat(successAudit.getTableName()).isEqualTo("projects");
        assertThat(successAudit.getRecordId()).isEqualTo(TARGET_PROJECT_ID);
        assertThat(successAudit.getOldValue()).isEqualTo("sourceProjectId=" + SOURCE_PROJECT_ID);
        assertThat(successAudit.getNewValue()).contains("clonedTasksCount=1");
    }

    // ==========================================
    // EDGE CASES
    // ==========================================
    @Test
    @DisplayName("Trường hợp biên: Báo lỗi khi nhân bản từ chính dự án này")
    void testCloneWbs_SameProject_ThrowsException() {
        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, TARGET_PROJECT_ID);

        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(CannotCloneFromSameProjectException.class)
                .hasMessageContaining("Không thể nhân bản cây công việc từ chính dự án này");

        verify(saveTaskPort, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Trường hợp biên: Báo lỗi khi dự án nguồn rỗng không có công việc")
    void testCloneWbs_EmptySourceProject_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-OLD", MANAGER_ID);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));
        when(loadTaskPort.findAllByProjectId(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(List.of()); // Không có task

        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);

        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(EmptySourceWbsException.class)
                .hasMessageContaining("chưa có cây công việc nào để nhân bản");
    }

    @Test
    @DisplayName("Trường hợp biên: Chặn nhân bản khi dự án đích đã đóng (CLOSED)")
    void testCloneWbs_TargetProjectClosed_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project closedTargetProject = new Project(
                new ProjectId(TARGET_PROJECT_ID),
                "PRJ-CLOSED",
                "Dự án đã đóng",
                10L,
                new EmployeeId(MANAGER_ID),
                LocalDate.now().minusMonths(6),
                LocalDate.now().minusDays(1),
                new BigDecimal("100.00"),
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                0);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(closedTargetProject));

        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);

        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(ProjectClosedException.class);
    }

    @Test
    @DisplayName("TC-02: Báo lỗi khi dự án nguồn không tồn tại (ProjectNotFoundException)")
    void testCloneWbs_SourceProjectNotFound_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.empty());

        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);

        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("Không tìm thấy dự án nguồn");

        verify(saveTaskPort, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("TC-03: Báo lỗi khi dự án đích không tồn tại (ProjectNotFoundException)")
    void testCloneWbs_TargetProjectNotFound_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.empty());

        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);

        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessageContaining("Không tìm thấy dự án đích");

        verify(saveTaskPort, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("TC-04: Báo lỗi khi không có quyền PROJECT_WBS_MANAGE")
    void testCloneWbs_NoPermission_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_WBS_MANAGE));

        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);

        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(loadProjectPort, never()).findByIdForUpdate(any());
        verify(saveTaskPort, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("TC-06: Người dùng có quyền quản lý Target nhưng Source project nằm ngoài Data Scope -> Bị từ chối và ghi log audit")
    void testCloneWbs_TC06_SourceProjectOutsideDataScope_ThrowsPermissionDeniedAndAudits() {
        // Given: User có DataScope.SELF, là PM của TARGET_PROJECT nhưng KHÔNG phải PM của SOURCE_PROJECT
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createSelfScopeUser(CURRENT_USER_ID)));

        Long myEmployeeId = 999L;
        Employee currentEmployee = new Employee(
                new EmployeeId(myEmployeeId),
                new UserId(CURRENT_USER_ID),
                10L,
                "EMP-999",
                "Nguyễn Văn PM",
                false,
                40,
                EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(currentEmployee));

        // Target project do chính user (myEmployeeId = 999L) quản lý -> ĐƯỢC PHÉP
        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-TARGET", myEmployeeId);
        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));

        // Source project do người khác quản lý (MANAGER_ID = 10L != 999L) -> NGOÀI DATA SCOPE
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-SOURCE", MANAGER_ID);
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));

        // When & Then
        CloneProjectWbsCommand command = new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID);
        assertThatThrownBy(() -> service.cloneWbs(command))
                .isInstanceOf(PermissionDeniedException.class);

        // Verify: Nhật ký từ chối truy cập được ghi nhận với đúng source project ID
        verify(saveDeniedAuditLogPort).save(auditLogCaptor.capture());
        AuditLog deniedAudit = auditLogCaptor.getValue();

        assertThat(deniedAudit.getAction()).isEqualTo("PROJECT_ACCESS_DENIED");
        assertThat(deniedAudit.getTableName()).isEqualTo("projects");
        assertThat(deniedAudit.getRecordId()).isEqualTo(SOURCE_PROJECT_ID);
        assertThat(deniedAudit.getNewValue()).contains("OUTSIDE_DATA_SCOPE_WBS_CLONE_SOURCE");

        // Verify: Không có task nào được load hay save
        verify(loadTaskPort, never()).findAllByProjectId(any());
        verify(saveTaskPort, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("TC-07: Sao chép cây công việc phân cấp sâu 4 tầng (Category -> Task -> Subtask -> Subtask) giữ nguyên cấu trúc")
    void testCloneWbs_TC07_FourLevelDeepHierarchy_ClonedCorrectly() {
        // Given
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-OLD", MANAGER_ID);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));

        ProjectId srcPrjId = new ProjectId(SOURCE_PROJECT_ID);
        // Level 1: Category A (id=1, parent=null)
        Task catA = new Task(new TaskId(1L), srcPrjId, null, "SRC-T001", "Giai đoạn 1", "Mô tả",
                TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.TODO, 1, new UserId(99L), LocalDateTime.now(), null, 0L);
        // Level 2: Task B (id=2, parent=1)
        Task taskB = new Task(new TaskId(2L), srcPrjId, new TaskId(1L), "SRC-T002", "Công việc B", "Mô tả",
                TaskType.TASK, new EmployeeId(50L), new BigDecimal("10.0"), BigDecimal.ZERO,
                new BigDecimal("15.0"), TaskStatus.TODO, 1, new UserId(99L), LocalDateTime.now(), null, 0L);
        // Level 3: Subtask C (id=3, parent=2)
        Task subtaskC = new Task(new TaskId(3L), srcPrjId, new TaskId(2L), "SRC-T003", "Công việc con C", "Mô tả",
                TaskType.TASK, new EmployeeId(51L), new BigDecimal("5.0"), BigDecimal.ZERO,
                new BigDecimal("8.0"), TaskStatus.TODO, 1, new UserId(99L), LocalDateTime.now(), null, 0L);
        // Level 4: Subtask D (id=4, parent=3)
        Task subtaskD = new Task(new TaskId(4L), srcPrjId, new TaskId(3L), "SRC-T004", "Công việc cháu D", "Mô tả",
                TaskType.TASK, new EmployeeId(52L), new BigDecimal("2.0"), BigDecimal.ZERO,
                new BigDecimal("4.0"), TaskStatus.TODO, 1, new UserId(99L), LocalDateTime.now(), null, 0L);

        when(loadTaskPort.findAllByProjectId(srcPrjId)).thenReturn(List.of(catA, taskB, subtaskC, subtaskD));

        AtomicLong generatedId = new AtomicLong(5000L);
        when(saveTaskPort.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return new Task(
                    new TaskId(generatedId.incrementAndGet()),
                    t.getProjectId(),
                    t.getParentId(),
                    t.getTaskCode(),
                    t.getName(),
                    t.getDescription(),
                    t.getTaskType(),
                    t.getAssigneeId(),
                    t.getEstimatedHours(),
                    t.getActualHours(),
                    t.getBudgetHours(),
                    t.getStatus(),
                    t.getSortOrder(),
                    t.getCreatedBy(),
                    LocalDateTime.now(),
                    null,
                    0L);
        });

        // When
        CloneProjectWbsResult result = service.cloneWbs(new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID));

        // Then
        assertThat(result.totalClonedTasks()).isEqualTo(4);
        assertThat(result.totalCategories()).isEqualTo(1);

        verify(saveTaskPort, times(4)).save(taskCaptor.capture());
        List<Task> saved = taskCaptor.getAllValues();

        Task clonedA = saved.get(0);
        Task clonedB = saved.get(1);
        Task clonedC = saved.get(2);
        Task clonedD = saved.get(3);

        assertThat(clonedA.getName()).isEqualTo("Giai đoạn 1");
        assertThat(clonedA.getParentId()).isNull();

        assertThat(clonedB.getName()).isEqualTo("Công việc B");
        assertThat(clonedB.getParentIdValue()).isEqualTo(5001L); // A's generated ID

        assertThat(clonedC.getName()).isEqualTo("Công việc con C");
        assertThat(clonedC.getParentIdValue()).isEqualTo(5002L); // B's generated ID

        assertThat(clonedD.getName()).isEqualTo("Công việc cháu D");
        assertThat(clonedD.getParentIdValue()).isEqualTo(5003L); // C's generated ID
    }

    @Test
    @DisplayName("TC-08: sortOrder không theo hierarchy (Grandchild có sortOrder nhỏ hơn Parent) -> Cấu trúc cây sau clone vẫn hoàn toàn chính xác")
    void testCloneWbs_TC08_SortOrderDoesNotFollowHierarchy_ClonedCorrectly() {
        // Given
        when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));

        Project targetProject = createActiveProject(TARGET_PROJECT_ID, "PRJ-NEW", MANAGER_ID);
        Project sourceProject = createActiveProject(SOURCE_PROJECT_ID, "PRJ-OLD", MANAGER_ID);

        when(loadProjectPort.findByIdForUpdate(new ProjectId(TARGET_PROJECT_ID))).thenReturn(Optional.of(targetProject));
        when(loadProjectPort.findById(new ProjectId(SOURCE_PROJECT_ID))).thenReturn(Optional.of(sourceProject));

        ProjectId srcPrjId = new ProjectId(SOURCE_PROJECT_ID);
        // Parent A có sortOrder = 10
        Task catA = new Task(new TaskId(1L), srcPrjId, null, "SRC-T001", "Category A", null,
                TaskType.CATEGORY, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.TODO, 10, new UserId(99L), LocalDateTime.now(), null, 0L);

        // Child B có sortOrder = 2
        Task taskB = new Task(new TaskId(2L), srcPrjId, new TaskId(1L), "SRC-T002", "Task B", null,
                TaskType.TASK, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.TODO, 2, new UserId(99L), LocalDateTime.now(), null, 0L);

        // Grandchild C có sortOrder = 1 (nhỏ hơn cả Parent A và Child B)
        Task subtaskC = new Task(new TaskId(3L), srcPrjId, new TaskId(2L), "SRC-T003", "Subtask C", null,
                TaskType.TASK, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                TaskStatus.TODO, 1, new UserId(99L), LocalDateTime.now(), null, 0L);

        // Đưa vào danh sách không theo thứ tự cây: C đứng trước, B đứng sau
        when(loadTaskPort.findAllByProjectId(srcPrjId)).thenReturn(List.of(subtaskC, catA, taskB));

        AtomicLong generatedId = new AtomicLong(8000L);
        when(saveTaskPort.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return new Task(
                    new TaskId(generatedId.incrementAndGet()),
                    t.getProjectId(),
                    t.getParentId(),
                    t.getTaskCode(),
                    t.getName(),
                    t.getDescription(),
                    t.getTaskType(),
                    t.getAssigneeId(),
                    t.getEstimatedHours(),
                    t.getActualHours(),
                    t.getBudgetHours(),
                    t.getStatus(),
                    t.getSortOrder(),
                    t.getCreatedBy(),
                    LocalDateTime.now(),
                    null,
                    0L);
        });

        // When
        CloneProjectWbsResult result = service.cloneWbs(new CloneProjectWbsCommand(TARGET_PROJECT_ID, SOURCE_PROJECT_ID));

        // Then
        assertThat(result.totalClonedTasks()).isEqualTo(3);
        verify(saveTaskPort, times(3)).save(taskCaptor.capture());
        List<Task> saved = taskCaptor.getAllValues();

        // Đảm bảo Parent A được save đầu tiên (trước B và C) bất chấp sortOrder của C < B < A
        Task clonedA = saved.get(0);
        Task clonedB = saved.get(1);
        Task clonedC = saved.get(2);

        assertThat(clonedA.getName()).isEqualTo("Category A");
        assertThat(clonedA.getParentId()).isNull();

        assertThat(clonedB.getName()).isEqualTo("Task B");
        assertThat(clonedB.getParentIdValue()).isEqualTo(8001L); // A's ID

        assertThat(clonedC.getName()).isEqualTo("Subtask C");
        assertThat(clonedC.getParentIdValue()).isEqualTo(8002L); // B's ID
    }
}
