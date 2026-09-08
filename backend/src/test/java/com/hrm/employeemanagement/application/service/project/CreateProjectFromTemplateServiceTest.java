package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.projecttemplate.LoadProjectTemplatePort;
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
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTaskId;
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
class CreateProjectFromTemplateServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long ORG_UNIT_ID = 100L;
    private static final Long MANAGER_ID = 50L;
    private static final Long TEMPLATE_ID = 1L;

    @Mock
    private LoadProjectTemplatePort loadProjectTemplatePort;

    @Mock
    private SaveProjectPort saveProjectPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

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

    private CreateProjectFromTemplateService service;

    @BeforeEach
    void setUp() {
        service = new CreateProjectFromTemplateService(
                loadProjectTemplatePort,
                saveProjectPort,
                saveTaskPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createAdminUser() {
        Role adminRole = new Role(new RoleId(1L), RoleCode.VT_06, "Quản trị viên");
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                "admin@hrm.com",
                null,
                1,
                0L);
    }

    private User createSelfUser() {
        Role employeeRole = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên");
        return new User(
                new UserId(CURRENT_USER_ID),
                "emp",
                "hash",
                employeeRole,
                UserStatus.ACTIVE,
                null,
                DataScope.SELF,
                null,
                "emp@hrm.com",
                null,
                1,
                0L);
    }

    private OrgUnit createOrgUnit(Long id, String code, OrgUnitStatus status) {
        return new OrgUnit(
                new OrgUnitId(id),
                code,
                "Phòng " + code,
                OrgUnitType.DEPARTMENT,
                null,
                "/" + id + "/",
                1,
                status,
                "Mô tả " + code,
                null,
                null,
                null);
    }

    private Employee createEmployee(Long id, EmployeeStatus status) {
        return new Employee(
                new EmployeeId(id),
                new UserId(CURRENT_USER_ID),
                ORG_UNIT_ID,
                "EMP001",
                "Nguyen Van PM",
                false,
                40,
                status);
    }

    private ProjectTemplate createTemplate(boolean active) {
        return new ProjectTemplate(
                new ProjectTemplateId(TEMPLATE_ID),
                "TPL-DEV-001",
                "Mẫu phát triển phần mềm",
                "Mô tả mẫu",
                active,
                new UserId(1L),
                LocalDateTime.now(),
                null,
                0L);
    }

    private List<ProjectTemplateTask> createTemplateTasks() {
        ProjectTemplateTask category = new ProjectTemplateTask(
                new ProjectTemplateTaskId(101L),
                new ProjectTemplateId(TEMPLATE_ID),
                null,
                "Giai đoạn Khảo sát",
                "Hạng mục khảo sát",
                TaskType.CATEGORY,
                BigDecimal.ZERO,
                1,
                LocalDateTime.now());

        ProjectTemplateTask task1 = new ProjectTemplateTask(
                new ProjectTemplateTaskId(102L),
                new ProjectTemplateId(TEMPLATE_ID),
                new ProjectTemplateTaskId(101L),
                "Khảo sát hiện trạng",
                "Task khảo sát",
                TaskType.TASK,
                new BigDecimal("8.00"),
                1,
                LocalDateTime.now());

        ProjectTemplateTask task2 = new ProjectTemplateTask(
                new ProjectTemplateTaskId(103L),
                new ProjectTemplateId(TEMPLATE_ID),
                new ProjectTemplateTaskId(101L),
                "Viết tài liệu đặc tả",
                "Task viết spec",
                TaskType.TASK,
                new BigDecimal("12.00"),
                2,
                LocalDateTime.now());

        return List.of(category, task1, task2);
    }

    @Test
    @DisplayName("TC-01: Tạo dự án từ mẫu thành công, tự động cộng tổng ngân sách giờ và nhân bản WBS")
    void testCreateProjectFromTemplate_Success_TC01() {
        // Arrange
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID,
                "Dự án CRM Mới",
                ORG_UNIT_ID,
                MANAGER_ID,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 12, 31),
                "Mô tả dự án CRM");

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadEmployeePort.findById(new EmployeeId(MANAGER_ID)))
                .thenReturn(Optional.of(createEmployee(MANAGER_ID, EmployeeStatus.ACTIVE)));

        when(loadProjectTemplatePort.findById(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(Optional.of(createTemplate(true)));
        when(loadProjectTemplatePort.findTasksByTemplateId(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(createTemplateTasks());

        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            if (p.getId() == null) {
                return new Project(
                        new ProjectId(555L),
                        p.getProjectCode(),
                        p.getProjectName(),
                        p.getOrgUnitId(),
                        p.getManagerId(),
                        p.getStartDate(),
                        p.getEndDate(),
                        p.getEstimatedHours(),
                        p.getDescription(),
                        ProjectStatus.ACTIVE,
                        p.getCreatedBy(),
                        LocalDateTime.now(),
                        null,
                        0L,
                        p.getTaskSeqCounter());
            }
            return p;
        });

        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            return new Task(
                    new TaskId(System.nanoTime()),
                    t.getProjectId(),
                    t.getParentId(),
                    t.getTaskCode(),
                    t.getName(),
                    t.getDescription(),
                    t.getTaskType(),
                    t.getAssigneeId(),
                    t.getEstimatedHours(),
                    t.getActualHours(),
                    t.getStatus(),
                    t.getSortOrder(),
                    t.getCreatedBy(),
                    LocalDateTime.now(),
                    null,
                    0L);
        });

        // Act
        ProjectResult result = service.createProjectFromTemplate(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(555L);
        assertThat(result.getProjectName()).isEqualTo("Dự án CRM Mới");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE); // Thỏa mãn QTN-04
        // Tổng giờ tự động cộng từ 2 tasks (8 + 12 = 20)
        assertThat(result.getEstimatedHours()).isEqualByComparingTo(new BigDecimal("20.00"));

        // Kiểm tra lưu 3 tasks (1 Category, 2 Tasks)
        verify(saveTaskPort, times(3)).save(any(Task.class));

        // Kiểm tra task con nhận parentTaskId chính xác, assigneeId là null, status là TODO
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(saveTaskPort, times(3)).save(taskCaptor.capture());
        List<Task> savedTasks = taskCaptor.getAllValues();

        Task cat = savedTasks.stream().filter(t -> t.getTaskType() == TaskType.CATEGORY).findFirst().orElseThrow();
        assertThat(cat.getParentId()).isNull();
        assertThat(cat.getName()).isEqualTo("Giai đoạn Khảo sát");

        List<Task> tasks = savedTasks.stream().filter(t -> t.getTaskType() == TaskType.TASK).toList();
        assertThat(tasks).hasSize(2);
        for (Task t : tasks) {
            assertThat(t.getAssigneeId()).isNull();
            assertThat(t.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(t.getActualHours()).isEqualTo(BigDecimal.ZERO);
            assertThat(t.getTaskCode()).startsWith("PRJ-IT-");
        }
    }

    @Test
    @DisplayName("TC-02: Độc lập dữ liệu (Deep Copy) - Các task mới hoàn toàn tách rời template gốc")
    void testDeepCopyIndependence_TC02() {
        // Arrange
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án Độc Lập", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));

        List<ProjectTemplateTask> originalTemplateTasks = createTemplateTasks();
        when(loadProjectTemplatePort.findById(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(Optional.of(createTemplate(true)));
        when(loadProjectTemplatePort.findTasksByTemplateId(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(originalTemplateTasks);

        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            return new Project(new ProjectId(999L), p.getProjectCode(), p.getProjectName(), p.getOrgUnitId(), null,
                    null, null, p.getEstimatedHours(), null, ProjectStatus.ACTIVE, p.getCreatedBy(), LocalDateTime.now(), null, 0L, 0);
        });

        when(saveTaskPort.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            return new Task(new TaskId(888L), t.getProjectId(), t.getParentId(), t.getTaskCode(), t.getName(),
                    t.getDescription(), t.getTaskType(), null, t.getEstimatedHours(), BigDecimal.ZERO, TaskStatus.TODO,
                    t.getSortOrder(), t.getCreatedBy(), LocalDateTime.now(), null, 0L);
        });

        // Act
        service.createProjectFromTemplate(command);

        // Assert: Dữ liệu mẫu gốc không bị thay đổi bất kỳ trường nào
        assertThat(originalTemplateTasks.get(0).getName()).isEqualTo("Giai đoạn Khảo sát");
        assertThat(originalTemplateTasks.get(1).getEstimatedHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(originalTemplateTasks.get(2).getEstimatedHours()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("TC-03: Người dùng không có quyền bị từ chối truy cập")
    void testPermissionDenied_TC03() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án Không Quyền", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_CREATE));

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("TC-03: Người dùng vi phạm Data Scope bị từ chối và ghi nhật ký PROJECT_ACCESS_DENIED")
    void testDataScopeDenied_TC03() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án Ngoài Scope", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createSelfUser()));

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(PermissionDeniedException.class);

        // Kiểm tra ghi nhật ký từ chối qua transaction độc lập
        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-04: Ghi nhận nhật ký kiểm toán CREATE_PROJECT_FROM_TEMPLATE sau khi tạo thành công")
    void testAuditLogSaved_TC04() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án Kiểm Toán", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadProjectTemplatePort.findById(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(Optional.of(createTemplate(true)));
        when(loadProjectTemplatePort.findTasksByTemplateId(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(List.of());

        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            return new Project(new ProjectId(777L), p.getProjectCode(), p.getProjectName(), p.getOrgUnitId(), null,
                    null, null, p.getEstimatedHours(), null, ProjectStatus.ACTIVE, p.getCreatedBy(), LocalDateTime.now(), null, 0L, 0);
        });

        service.createProjectFromTemplate(command);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog logged = auditCaptor.getValue();

        assertThat(logged.getAction()).isEqualTo("CREATE_PROJECT_FROM_TEMPLATE");
        assertThat(logged.getTableName()).isEqualTo("projects");
        assertThat(logged.getRecordId()).isEqualTo(777L);
        assertThat(logged.getNewValue()).contains("templateId=1");
        assertThat(logged.getNewValue()).contains("templateCode=TPL-DEV-001");
    }

    @Test
    @DisplayName("Mẫu dự án không tồn tại sẽ ném ProjectTemplateNotFoundException (404)")
    void testTemplateNotFound_ThrowsException() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                9999L, "Dự án Lỗi Mẫu", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadProjectTemplatePort.findById(new ProjectTemplateId(9999L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(ProjectTemplateNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    @DisplayName("Mẫu dự án bị vô hiệu hóa (inactive) sẽ ném ProjectTemplateNotFoundException")
    void testTemplateInactive_ThrowsException() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án Lỗi Mẫu Inactive", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadProjectTemplatePort.findById(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(Optional.of(createTemplate(false)));

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(ProjectTemplateNotFoundException.class)
                .hasMessageContaining("vô hiệu hóa");
    }

    @Test
    @DisplayName("Quản lý dự án (PM) không ở trạng thái hoạt động sẽ ném InvalidProjectDataException")
    void testManagerNotActive_ThrowsException() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án PM Inactive", ORG_UNIT_ID, MANAGER_ID, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));

        when(loadEmployeePort.findById(new EmployeeId(MANAGER_ID)))
                .thenReturn(Optional.of(createEmployee(MANAGER_ID, EmployeeStatus.TERMINATED)));

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("hoạt động");
    }

    @Test
    @DisplayName("Template WBS chứa task có cha không thuộc template sẽ ném InvalidProjectDataException")
    void testTemplateWbs_TaskParentOutsideTemplate_ThrowsException() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án WBS Lỗi Parent", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadProjectTemplatePort.findById(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(Optional.of(createTemplate(true)));

        // Task con có parentId = 999 không nằm trong danh sách task của template này
        List<ProjectTemplateTask> corruptedTasks = List.of(
                new ProjectTemplateTask(
                        new ProjectTemplateTaskId(1L),
                        new ProjectTemplateId(TEMPLATE_ID),
                        new ProjectTemplateTaskId(999L),
                        "Task mồ côi ngoài template",
                        "Desc",
                        TaskType.TASK,
                        BigDecimal.TEN,
                        1,
                        LocalDateTime.now()));
        when(loadProjectTemplatePort.findTasksByTemplateId(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(corruptedTasks);

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("không thuộc cùng mẫu dự án");
    }

    @Test
    @DisplayName("Template WBS chứa chu trình phụ thuộc vòng sẽ ném InvalidProjectDataException và rollback")
    void testTemplateWbs_CyclicDependency_ThrowsException() {
        CreateProjectFromTemplateCommand command = new CreateProjectFromTemplateCommand(
                TEMPLATE_ID, "Dự án WBS Chu Trình Vòng", ORG_UNIT_ID, null, null, null, null);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID)))
                .thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadProjectTemplatePort.findById(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(Optional.of(createTemplate(true)));

        // Task 1 trỏ Task 2, Task 2 trỏ Task 1 -> Vòng lặp
        List<ProjectTemplateTask> cyclicTasks = List.of(
                new ProjectTemplateTask(
                        new ProjectTemplateTaskId(1L),
                        new ProjectTemplateId(TEMPLATE_ID),
                        new ProjectTemplateTaskId(2L),
                        "Task 1",
                        "Desc",
                        TaskType.TASK,
                        BigDecimal.TEN,
                        1,
                        LocalDateTime.now()),
                new ProjectTemplateTask(
                        new ProjectTemplateTaskId(2L),
                        new ProjectTemplateId(TEMPLATE_ID),
                        new ProjectTemplateTaskId(1L),
                        "Task 2",
                        "Desc",
                        TaskType.TASK,
                        BigDecimal.TEN,
                        2,
                        LocalDateTime.now()));
        when(loadProjectTemplatePort.findTasksByTemplateId(new ProjectTemplateId(TEMPLATE_ID)))
                .thenReturn(cyclicTasks);

        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            return new Project(new ProjectId(101L), p.getProjectCode(), p.getProjectName(), p.getOrgUnitId(), null,
                    null, null, p.getEstimatedHours(), null, ProjectStatus.ACTIVE, p.getCreatedBy(), LocalDateTime.now(), null, 0L, 0);
        });

        assertThatThrownBy(() -> service.createProjectFromTemplate(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("tồn tại công việc mồ côi hoặc bị phụ thuộc vòng lặp");
    }

    @Test
    @DisplayName("ProjectTemplateId và ProjectTemplateTaskId từ chối giá trị <= 0")
    void testNegativeOrZeroIds_ThrowIllegalArgumentException() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ProjectTemplateId(0L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ProjectTemplateId(-5L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ProjectTemplateTaskId(0L));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new ProjectTemplateTaskId(-1L));
    }
}
