package com.hrm.employeemanagement.application.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.milestone.CreateMilestoneCommand;
import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.SaveMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
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
import com.hrm.employeemanagement.domain.exception.milestone.DuplicateMilestoneNameException;
import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.exception.milestone.ProjectHasNoWbsException;
import com.hrm.employeemanagement.domain.exception.milestone.TaskNotInProjectException;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;
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
class CreateMilestoneServiceTest {

    @Mock
    private LoadMilestonePort loadMilestonePort;
    @Mock
    private SaveMilestonePort saveMilestonePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
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

    private CreateMilestoneService service;

    private static final Long CURRENT_USER_ID = 100L;
    private static final Long PM_EMPLOYEE_ID = 50L;
    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new CreateMilestoneService(
                loadMilestonePort,
                saveMilestonePort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createPmUser() {
        return new User(
                new UserId(CURRENT_USER_ID),
                "pm_user",
                "hashed",
                new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private Project createActiveProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-001",
                "Dự án ERP",
                10L,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                BigDecimal.valueOf(100),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    private Task createTask(Long taskId, Long projectId) {
        return new Task(
                new TaskId(taskId),
                new ProjectId(projectId),
                null,
                "T-001",
                "Công việc 1",
                "Mô tả",
                TaskType.TASK,
                new EmployeeId(PM_EMPLOYEE_ID),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                TaskStatus.TODO,
                0,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    @Test
    @DisplayName("TC-01: Tạo mốc thành công khi dự án đã có cây công việc WBS")
    void shouldCreateMilestoneSuccessfullyWhenProjectHasWbs() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task task = createTask(101L, PROJECT_ID);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(task));
        when(loadMilestonePort.existsByProjectIdAndName(new ProjectId(PROJECT_ID), "Mốc bàn giao đợt 1")).thenReturn(false);

        when(saveMilestonePort.save(any(Milestone.class))).thenAnswer(inv -> {
            Milestone arg = inv.getArgument(0);
            return new Milestone(
                    new MilestoneId(1L),
                    arg.getProjectId(),
                    arg.getName(),
                    arg.getDescription(),
                    arg.getPlannedDate(),
                    arg.getActualDate(),
                    arg.getLinkedTaskIds(),
                    arg.getCreatedBy(),
                    LocalDateTime.now(),
                    null,
                    0L);
        });

        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "Mốc bàn giao đợt 1",
                "Hoàn thành WBS đợt 1",
                LocalDate.now().plusDays(15),
                List.of(101L));

        MilestoneResult result = service.createMilestone(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Mốc bàn giao đợt 1");
        assertThat(result.status()).isEqualTo(MilestoneStatus.ON_TRACK);
        assertThat(result.totalLinkedTasks()).isEqualTo(1);
        assertThat(result.completedLinkedTasks()).isEqualTo(0);

        // Verify Audit Log (TC-04)
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Precondition: Ném ProjectHasNoWbsException khi dự án chưa có bất kỳ công việc nào trong WBS")
    void shouldThrowProjectHasNoWbsExceptionWhenProjectHasNoTasks() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of()); // WBS rỗng

        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "Mốc bàn giao đợt 1",
                null,
                LocalDate.now().plusDays(15),
                List.of());

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(ProjectHasNoWbsException.class)
                .hasMessageContaining("chưa có cây công việc (WBS)");

        verify(saveMilestonePort, never()).save(any());
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Từ chối truy cập và ghi nhật ký từ chối khi người dùng không đủ quyền")
    void shouldDenyAccessAndLogAuditWhenUserLacksPermission() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_MILESTONE_MANAGE));

        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "Mốc bàn giao đợt 1",
                null,
                LocalDate.now().plusDays(15),
                List.of());

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveMilestonePort, never()).save(any());
    }

    @Test
    @DisplayName("Ném DuplicateMilestoneNameException khi tên mốc đã tồn tại trong dự án")
    void shouldThrowDuplicateMilestoneNameExceptionWhenNameExists() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task task = createTask(101L, PROJECT_ID);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(task));
        when(loadMilestonePort.existsByProjectIdAndName(new ProjectId(PROJECT_ID), "Mốc trùng")).thenReturn(true);

        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "Mốc trùng",
                null,
                LocalDate.now().plusDays(15),
                List.of());

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(DuplicateMilestoneNameException.class)
                .hasMessageContaining("Tên mốc tiến độ đã tồn tại");
    }

    @Test
    @DisplayName("Ném TaskNotInProjectException khi gắn task thuộc dự án khác")
    void shouldThrowTaskNotInProjectExceptionWhenLinkedTaskFromOtherProject() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task task = createTask(101L, PROJECT_ID);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(task));
        when(loadMilestonePort.existsByProjectIdAndName(new ProjectId(PROJECT_ID), "Mốc mới")).thenReturn(false);

        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "Mốc mới",
                null,
                LocalDate.now().plusDays(15),
                List.of(999L)); // 999L không thuộc PROJECT_ID

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(TaskNotInProjectException.class)
                .hasMessageContaining("không thuộc dự án");
    }

    @Test
    @DisplayName("Khởi tạo mốc quá hạn ngày kế hoạch thì trạng thái lưu xuống DB được tính là DELAYED")
    void shouldPersistDelayedStatusWhenCreatingPastPlannedDate() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Task task = createTask(101L, PROJECT_ID);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(task));
        when(loadMilestonePort.existsByProjectIdAndName(new ProjectId(PROJECT_ID), "Mốc trễ")).thenReturn(false);

        when(saveMilestonePort.save(any(Milestone.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "Mốc trễ",
                null,
                LocalDate.now().minusDays(5),
                List.of(101L));

        MilestoneResult result = service.createMilestone(command);

        assertThat(result.status()).isEqualTo(MilestoneStatus.DELAYED);
        assertThat(result.delayDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi command.name() là null")
    void shouldThrowInvalidMilestoneDataExceptionWhenCommandNameIsNull() {
        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                null,
                "Mô tả",
                LocalDate.now().plusDays(15),
                List.of());

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Tên mốc tiến độ không được để trống");
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi command.name() là khoảng trắng")
    void shouldThrowInvalidMilestoneDataExceptionWhenCommandNameIsBlank() {
        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "    ",
                "Mô tả",
                LocalDate.now().plusDays(15),
                List.of());

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Tên mốc tiến độ không được để trống");
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi command.name() vượt quá 255 ký tự")
    void shouldThrowInvalidMilestoneDataExceptionWhenCommandNameIsTooLong() {
        CreateMilestoneCommand command = new CreateMilestoneCommand(
                PROJECT_ID,
                "A".repeat(256),
                "Mô tả",
                LocalDate.now().plusDays(15),
                List.of());

        assertThatThrownBy(() -> service.createMilestone(command))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Tên mốc tiến độ không được vượt quá 255 ký tự");
    }
}
