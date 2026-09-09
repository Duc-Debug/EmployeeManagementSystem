package com.hrm.employeemanagement.application.service.milestone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
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
class GetProjectMilestonesServiceTest {

    @Mock
    private LoadMilestonePort loadMilestonePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private GetProjectMilestonesService service;

    private static final Long CURRENT_USER_ID = 100L;
    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new GetProjectMilestonesService(
                loadMilestonePort,
                loadProjectPort,
                loadTaskPort,
                loadEmployeePort,
                loadUserPort,
                saveDeniedAuditLogPort,
                authorizationService);
    }

    private User createReaderUser() {
        return new User(
                new UserId(CURRENT_USER_ID),
                "reader_user",
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
                LocalDate.now().minusMonths(2),
                LocalDate.now().plusMonths(4),
                BigDecimal.valueOf(100),
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);
    }

    @Test
    @DisplayName("TC-02: Rà soát tiến độ nhận diện mốc quá hạn (DELAYED) kèm số ngày trễ chính xác")
    void shouldDetectDelayedMilestoneWithExactDays() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createReaderUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        LocalDate pastDate = LocalDate.now().minusDays(5);
        Milestone overdueMilestone = new Milestone(
                new MilestoneId(1L),
                new ProjectId(PROJECT_ID),
                "Mốc thiết kế DB",
                "Hoàn thành DB",
                pastDate,
                null,
                Set.of(new TaskId(10L)),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now().minusMonths(1),
                null,
                0L);

        when(loadMilestonePort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(overdueMilestone));

        Task ongoingTask = new Task(
                new TaskId(10L),
                new ProjectId(PROJECT_ID),
                null,
                "T-01",
                "Thiết kế ERD",
                null,
                TaskType.TASK,
                new EmployeeId(50L),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(4),
                TaskStatus.IN_PROGRESS, // Chưa hoàn thành!
                0,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(ongoingTask));

        List<MilestoneResult> results = service.getProjectMilestones(PROJECT_ID);

        assertThat(results).hasSize(1);
        MilestoneResult res = results.get(0);
        assertThat(res.status()).isEqualTo(MilestoneStatus.DELAYED);
        assertThat(res.delayDays()).isEqualTo(5); // Kèm số ngày trễ (TC-02)
        assertThat(res.completedLinkedTasks()).isEqualTo(0);
        assertThat(res.totalLinkedTasks()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mốc chưa đến hạn được đánh dấu ON_TRACK và delayDays = 0")
    void shouldReturnOnTrackWhenBeforePlannedDate() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createReaderUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        LocalDate futureDate = LocalDate.now().plusDays(10);
        Milestone futureMilestone = new Milestone(
                new MilestoneId(2L),
                new ProjectId(PROJECT_ID),
                "Mốc kiểm thử",
                null,
                futureDate,
                null,
                Set.of(new TaskId(20L)),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(futureMilestone));
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of());

        List<MilestoneResult> results = service.getProjectMilestones(PROJECT_ID);

        assertThat(results).hasSize(1);
        MilestoneResult res = results.get(0);
        assertThat(res.status()).isEqualTo(MilestoneStatus.ON_TRACK);
        assertThat(res.delayDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("Mốc hoàn thành (COMPLETED) khi toàn bộ task liên kết đã DONE")
    void shouldReturnCompletedWhenAllLinkedTasksDone() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createReaderUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));

        LocalDate pastDate = LocalDate.now().minusDays(2);
        Milestone milestone = new Milestone(
                new MilestoneId(3L),
                new ProjectId(PROJECT_ID),
                "Mốc bàn giao giai đoạn một",
                null,
                pastDate,
                null,
                Set.of(new TaskId(30L)),
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        Task completedTask = new Task(
                new TaskId(30L),
                new ProjectId(PROJECT_ID),
                null,
                "T-30",
                "Task đã xong",
                null,
                TaskType.TASK,
                new EmployeeId(50L),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(8),
                TaskStatus.DONE, // Đã hoàn thành!
                0,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L);

        when(loadMilestonePort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(milestone));
        when(loadTaskPort.findAllByProjectId(new ProjectId(PROJECT_ID))).thenReturn(List.of(completedTask));

        List<MilestoneResult> results = service.getProjectMilestones(PROJECT_ID);

        assertThat(results).hasSize(1);
        MilestoneResult res = results.get(0);
        assertThat(res.status()).isEqualTo(MilestoneStatus.COMPLETED);
        assertThat(res.delayDays()).isEqualTo(0);
        assertThat(res.completedLinkedTasks()).isEqualTo(1);
        assertThat(res.totalLinkedTasks()).isEqualTo(1);
    }
}
