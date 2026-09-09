package com.hrm.employeemanagement.application.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.dto.milestone.UpdateMilestoneCommand;
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
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.exception.milestone.MilestoneNotFoundException;
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
class UpdateMilestoneServiceTest {

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

    private UpdateMilestoneService service;

    private static final Long CURRENT_USER_ID = 100L;
    private static final Long PROJECT_ID = 1L;
    private static final Long MILESTONE_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new UpdateMilestoneService(
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
                new EmployeeId(50L),
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

    @Test
    @DisplayName("Cập nhật mốc thành công và ghi nhận nhật ký kiểm toán (TC-04)")
    void shouldUpdateMilestoneSuccessfullyAndLogAudit() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Milestone existing = new Milestone(
                new MilestoneId(MILESTONE_ID),
                new ProjectId(PROJECT_ID),
                "Tên cũ",
                "Mô tả cũ",
                LocalDate.now().plusDays(10),
                null,
                Set.of(),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findById(new MilestoneId(MILESTONE_ID))).thenReturn(Optional.of(existing));
        when(loadMilestonePort.existsByProjectIdAndName(new ProjectId(PROJECT_ID), "Tên mới")).thenReturn(false);
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of());
        when(saveMilestonePort.save(any(Milestone.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMilestoneCommand command = new UpdateMilestoneCommand(
                PROJECT_ID,
                MILESTONE_ID,
                "Tên mới",
                "Mô tả mới",
                LocalDate.now().plusDays(20),
                null,
                List.of());

        MilestoneResult result = service.updateMilestone(command);

        assertThat(result.name()).isEqualTo("Tên mới");
        assertThat(result.description()).isEqualTo("Mô tả mới");

        // Verify Audit Log (TC-04)
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Ném MilestoneNotFoundException khi không tìm thấy mốc")
    void shouldThrowWhenMilestoneNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadMilestonePort.findById(new MilestoneId(999L))).thenReturn(Optional.empty());

        UpdateMilestoneCommand command = new UpdateMilestoneCommand(
                PROJECT_ID,
                999L,
                "Tên mới",
                null,
                LocalDate.now(),
                null,
                null);

        assertThatThrownBy(() -> service.updateMilestone(command))
                .isInstanceOf(MilestoneNotFoundException.class);
    }

    @Test
    @DisplayName("Trạng thái mốc được tự động tính toán từ các task WBS liên kết trước khi lưu vào DB")
    void shouldAutomaticallyEvaluateAndPersistConsistentStatus() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Milestone existing = new Milestone(
                new MilestoneId(MILESTONE_ID),
                new ProjectId(PROJECT_ID),
                "Mốc bàn giao",
                null,
                LocalDate.now().minusDays(3),
                null,
                Set.of(new TaskId(101L)),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findById(new MilestoneId(MILESTONE_ID))).thenReturn(Optional.of(existing));

        // Task 101 chưa hoàn thành (IN_PROGRESS)
        Task task1 = new Task(
                new TaskId(101L),
                new ProjectId(PROJECT_ID),
                null,
                "T-101",
                "Task 1",
                null,
                TaskType.TASK,
                new EmployeeId(10L),
                BigDecimal.valueOf(8),
                BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS,
                1,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(task1));
        when(saveMilestonePort.save(any(Milestone.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMilestoneCommand command = new UpdateMilestoneCommand(
                PROJECT_ID,
                MILESTONE_ID,
                "Mốc bàn giao",
                null,
                LocalDate.now().minusDays(3), // Quá hạn 3 ngày mà task chưa xong
                null,
                List.of(101L));

        MilestoneResult result = service.updateMilestone(command);

        // Trạng thái trả về phải được tính toán tự động là DELAYED
        assertThat(result.status()).isEqualTo(MilestoneStatus.DELAYED);
        assertThat(result.delayDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi actualDate cập nhật trước plannedDate")
    void shouldThrowInvalidMilestoneDataExceptionWhenActualDateIsBeforePlannedDate() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Milestone existing = new Milestone(
                new MilestoneId(MILESTONE_ID),
                new ProjectId(PROJECT_ID),
                "Mốc bàn giao",
                null,
                LocalDate.of(2026, 9, 20),
                null,
                Set.of(),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findById(new MilestoneId(MILESTONE_ID))).thenReturn(Optional.of(existing));

        UpdateMilestoneCommand command = new UpdateMilestoneCommand(
                PROJECT_ID,
                MILESTONE_ID,
                null,
                null,
                null,
                LocalDate.of(2026, 9, 10), // trước plannedDate (2026-09-20)
                null);

        assertThatThrownBy(() -> service.updateMilestone(command))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Ngày hoàn thành thực tế không được trước ngày kế hoạch");
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi cập nhật tên mốc là khoảng trắng")
    void shouldThrowInvalidMilestoneDataExceptionWhenUpdateNameIsBlank() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Milestone existing = new Milestone(
                new MilestoneId(MILESTONE_ID),
                new ProjectId(PROJECT_ID),
                "Mốc bàn giao",
                null,
                LocalDate.of(2026, 9, 20),
                null,
                Set.of(),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findById(new MilestoneId(MILESTONE_ID))).thenReturn(Optional.of(existing));

        UpdateMilestoneCommand command = new UpdateMilestoneCommand(
                PROJECT_ID,
                MILESTONE_ID,
                "   ",
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.updateMilestone(command))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Tên mốc tiến độ không được để trống");
    }

    @Test
    @DisplayName("Ném InvalidMilestoneDataException khi cập nhật tên mốc vượt quá 255 ký tự")
    void shouldThrowInvalidMilestoneDataExceptionWhenUpdateNameIsTooLong() {
        when(authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createPmUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        Milestone existing = new Milestone(
                new MilestoneId(MILESTONE_ID),
                new ProjectId(PROJECT_ID),
                "Mốc bàn giao",
                null,
                LocalDate.of(2026, 9, 20),
                null,
                Set.of(),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findById(new MilestoneId(MILESTONE_ID))).thenReturn(Optional.of(existing));

        UpdateMilestoneCommand command = new UpdateMilestoneCommand(
                PROJECT_ID,
                MILESTONE_ID,
                "A".repeat(256),
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.updateMilestone(command))
                .isInstanceOf(InvalidMilestoneDataException.class)
                .hasMessageContaining("Tên mốc tiến độ không được vượt quá 255 ký tự");
    }
}
