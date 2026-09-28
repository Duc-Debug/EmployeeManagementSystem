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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class GetProjectMembersServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long PM_EMPLOYEE_ID = 50L;
    private static final Long ORG_UNIT_ID = 100L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private GetProjectMembersService service;

    @BeforeEach
    void setUp() {
        service = new GetProjectMembersService(
                loadProjectPort,
                loadProjectMemberPort,
                loadUserPort,
                loadEmployeePort,
                saveDeniedAuditLogPort,
                authorizationService
        );
    }

    private User createTestUser(DataScope dataScope, Long orgUnitId) {
        RoleCode roleCode = (dataScope == DataScope.COMPANY) ? RoleCode.VT_06 : RoleCode.VT_04;
        return new User(
                new UserId(CURRENT_USER_ID),
                "testuser",
                "hash",
                new Role(new RoleId(1L), roleCode, "Role"),
                UserStatus.ACTIVE,
                new EmployeeId(PM_EMPLOYEE_ID),
                dataScope,
                orgUnitId,
                1L
        );
    }

    private Project createTestProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Test Project",
                ORG_UNIT_ID,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO,
                "Mô tả",
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );
    }

    @Test
    @DisplayName("Ném ngoại lệ khi projectId là null")
    void shouldThrowWhenProjectIdIsNull() {
        assertThatThrownBy(() -> service.getProjectMembers(null))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("projectId");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi không tìm thấy dự án")
    void shouldThrowWhenProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createTestUser(DataScope.COMPANY, null)));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProjectMembers(PROJECT_ID))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi user ngoài data scope (SELF nhưng không phải PM và không phải member)")
    void shouldThrowWhenOutsideDataScope() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createTestUser(DataScope.SELF, null)));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createTestProject()));

        Employee otherEmployee = new Employee(
                new EmployeeId(999L),
                new UserId(CURRENT_USER_ID),
                100L,
                "EMP-999",
                "Other User",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(otherEmployee));
        when(loadProjectPort.existsMember(PROJECT_ID, 999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getProjectMembers(PROJECT_ID))
                .isInstanceOf(PermissionDeniedException.class);
        verify(saveDeniedAuditLogPort).save(any());
        verify(loadProjectMemberPort, never()).findMembersByProjectId(any());
    }

    @Test
    @DisplayName("Lấy danh sách thành viên thành công khi user có COMPANY data scope")
    void shouldReturnMembersWhenCompanyScope() {
        when(authorizationService.require(PermissionCode.PROJECT_READ)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(createTestUser(DataScope.COMPANY, null)));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createTestProject()));

        List<ProjectMemberResult> expectedMembers = List.of(
                new ProjectMemberResult(50L, "EMP-050", "Manager Name", "pm@hrm.com", 100L, "Engineering", ProjectMemberRole.PROJECT_MANAGER, "ACTIVE"),
                new ProjectMemberResult(60L, "EMP-060", "Member Name", "member@hrm.com", 100L, "Engineering", ProjectMemberRole.MEMBER, "ACTIVE")
        );
        when(loadProjectMemberPort.findMembersByProjectId(PROJECT_ID)).thenReturn(expectedMembers);

        List<ProjectMemberResult> result = service.getProjectMembers(PROJECT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).roleInProject()).isEqualTo(ProjectMemberRole.PROJECT_MANAGER);
        assertThat(result.get(1).roleInProject()).isEqualTo(ProjectMemberRole.MEMBER);
    }
}
