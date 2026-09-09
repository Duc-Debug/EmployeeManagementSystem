package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.hrm.employeemanagement.application.dto.project.AddProjectMemberCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectMemberException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
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
class AddProjectMemberServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long PROJECT_ID = 1L;
    private static final Long PM_EMPLOYEE_ID = 50L;
    private static final Long NEW_MEMBER_EMPLOYEE_ID = 60L;
    private static final Long ORG_UNIT_ID = 100L;

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadProjectMemberPort loadProjectMemberPort;
    @Mock
    private SaveProjectMemberPort saveProjectMemberPort;
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

    private AddProjectMemberService service;

    @BeforeEach
    void setUp() {
        service = new AddProjectMemberService(
                loadProjectPort,
                loadProjectMemberPort,
                saveProjectMemberPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService
        );
    }

    private User createAdminUser() {
        return new User(
                new UserId(CURRENT_USER_ID),
                "admin",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_06, "Admin"),
                UserStatus.ACTIVE,
                new EmployeeId(PM_EMPLOYEE_ID),
                DataScope.COMPANY,
                null,
                1L
        );
    }

    private Project createActiveProject() {
        return new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Project Alpha",
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

    private Employee createActiveEmployee(Long employeeId) {
        return new Employee(
                new EmployeeId(employeeId),
                new UserId(200L),
                ORG_UNIT_ID,
                "EMP-0" + employeeId,
                "Employee Name",
                "Developer",
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusMonths(6),
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("Ném ngoại lệ khi command null hoặc thiếu thông tin")
    void shouldThrowWhenCommandIsInvalid() {
        assertThatThrownBy(() -> service.addProjectMember(null))
                .isInstanceOf(InvalidProjectDataException.class);

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(null, 60L)))
                .isInstanceOf(InvalidProjectDataException.class);

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(1L, null)))
                .isInstanceOf(InvalidProjectDataException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi không tìm thấy dự án")
    void shouldThrowWhenProjectNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi dự án đã CLOSED")
    void shouldThrowWhenProjectIsClosed() {
        Project closedProject = new Project(
                new ProjectId(PROJECT_ID),
                "PRJ-01",
                "Project Alpha",
                ORG_UNIT_ID,
                new EmployeeId(PM_EMPLOYEE_ID),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO,
                "Mô tả",
                ProjectStatus.CLOSED,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(closedProject));

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(ProjectClosedException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi không tìm thấy nhân viên")
    void shouldThrowWhenEmployeeNotFound() {
        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findById(new EmployeeId(NEW_MEMBER_EMPLOYEE_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nhân viên không ở trạng thái ACTIVE")
    void shouldThrowWhenEmployeeNotActive() {
        Employee terminatedEmployee = new Employee(
                new EmployeeId(NEW_MEMBER_EMPLOYEE_ID),
                new UserId(200L),
                ORG_UNIT_ID,
                "EMP-060",
                "Terminated Employee",
                "Developer",
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusMonths(6),
                false,
                40,
                EmployeeStatus.TERMINATED
        );

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findById(new EmployeeId(NEW_MEMBER_EMPLOYEE_ID))).thenReturn(Optional.of(terminatedEmployee));

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("hoạt động");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nhân sự đã hết hạn hợp đồng")
    void shouldThrowWhenContractExpired() {
        Employee expiredEmployee = new Employee(
                new EmployeeId(NEW_MEMBER_EMPLOYEE_ID),
                new UserId(200L),
                ORG_UNIT_ID,
                "EMP-060",
                "Expired Employee",
                "Developer",
                LocalDate.now().minusMonths(12),
                LocalDate.now().minusDays(1),
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findById(new EmployeeId(NEW_MEMBER_EMPLOYEE_ID))).thenReturn(Optional.of(expiredEmployee));

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("kết thúc hợp đồng");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nhân sự đã là PM của dự án")
    void shouldThrowWhenEmployeeIsPM() {
        Employee pmEmployee = createActiveEmployee(PM_EMPLOYEE_ID);

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findById(new EmployeeId(PM_EMPLOYEE_ID))).thenReturn(Optional.of(pmEmployee));

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, PM_EMPLOYEE_ID)))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("quản lý (PM)");
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nhân viên đã là thành viên của dự án")
    void shouldThrowWhenEmployeeAlreadyMember() {
        Employee member = createActiveEmployee(NEW_MEMBER_EMPLOYEE_ID);

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findById(new EmployeeId(NEW_MEMBER_EMPLOYEE_ID))).thenReturn(Optional.of(member));
        when(loadProjectMemberPort.existsMember(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)))
                .isInstanceOf(DuplicateProjectMemberException.class);
    }

    @Test
    @DisplayName("Thêm thành viên dự án thành công")
    void shouldAddMemberSuccessfully() {
        Employee member = createActiveEmployee(NEW_MEMBER_EMPLOYEE_ID);
        User memberUser = new User(
                new UserId(200L),
                "member_user",
                "hash",
                new Role(new RoleId(2L), RoleCode.VT_04, "Employee"),
                UserStatus.ACTIVE,
                new EmployeeId(NEW_MEMBER_EMPLOYEE_ID),
                DataScope.SELF,
                null,
                "member@hrm.com",
                null,
                1,
                1L
        );

        when(authorizationService.require(PermissionCode.PROJECT_UPDATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadProjectPort.findById(new ProjectId(PROJECT_ID))).thenReturn(Optional.of(createActiveProject()));
        when(loadEmployeePort.findById(new EmployeeId(NEW_MEMBER_EMPLOYEE_ID))).thenReturn(Optional.of(member));
        when(loadProjectMemberPort.existsMember(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID)).thenReturn(false);
        when(loadUserPort.findById(new UserId(200L))).thenReturn(Optional.of(memberUser));

        ProjectMemberResult result = service.addProjectMember(new AddProjectMemberCommand(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID));

        assertThat(result).isNotNull();
        assertThat(result.employeeId()).isEqualTo(NEW_MEMBER_EMPLOYEE_ID);
        assertThat(result.roleInProject()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(result.email()).isEqualTo("member@hrm.com");

        verify(saveProjectMemberPort).addMember(PROJECT_ID, NEW_MEMBER_EMPLOYEE_ID);
        verify(saveAuditLogPort).save(any());
    }
}
