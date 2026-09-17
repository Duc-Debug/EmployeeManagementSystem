package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.reservation.AutoProcessProjectReservationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class ApproveProjectServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;
    private static final Long MANAGER_ID = 50L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private SaveProjectPort saveProjectPort;
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
    @Mock
    private AutoProcessProjectReservationsUseCase autoProcessProjectReservationsUseCase;

    private ApproveProjectService approveProjectService;

    @BeforeEach
    void setUp() {
        approveProjectService = new ApproveProjectService(
                loadProjectPort,
                saveProjectPort,
                loadUserPort,
                loadEmployeePort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService,
                autoProcessProjectReservationsUseCase);
    }

    private User createExecutiveUser() {
        Role execRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc");
        return new User(
                new UserId(CURRENT_USER_ID),
                "director.user",
                "hashed_pwd",
                execRole,
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L);
    }

    private Project createPlannedProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-2026-001",
                "Dự án ERP Kế hoạch",
                ORG_UNIT_ID,
                new EmployeeId(MANAGER_ID),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("500.00"),
                "Mô tả dự án",
                ProjectStatus.PLANNED,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                0L);
    }

    @Test
    @DisplayName("Phê duyệt dự án thành công: Trạng thái chuyển sang ACTIVE và auto convert reservation được gọi")
    void approveProject_Success() {
        User user = createExecutiveUser();
        Project project = createPlannedProject();

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(user));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(project));
        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResult result = approveProjectService.approveProject(PROJECT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(saveProjectPort).save(any(Project.class));
        verify(autoProcessProjectReservationsUseCase).autoConvertForProject(PROJECT_ID);
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi dự án không tồn tại")
    void approveProject_NotFound_ThrowsException() {
        User user = createExecutiveUser();

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(user));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approveProjectService.approveProject(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(saveProjectPort, never()).save(any());
        verify(autoProcessProjectReservationsUseCase, never()).autoConvertForProject(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi dự án không ở trạng thái PLANNED")
    void approveProject_NotPlanned_ThrowsException() {
        User user = createExecutiveUser();
        Project activeProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-2026-001",
                "Dự án ERP",
                ORG_UNIT_ID,
                new EmployeeId(MANAGER_ID),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("500.00"),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                LocalDateTime.now(),
                0L);

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(user));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(activeProject));

        assertThatThrownBy(() -> approveProjectService.approveProject(PROJECT_ID))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("dự kiến");

        verify(saveProjectPort, never()).save(any());
        verify(autoProcessProjectReservationsUseCase, never()).autoConvertForProject(any());
    }
}
