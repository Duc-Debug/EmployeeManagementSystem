package com.hrm.employeemanagement.application.service.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
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
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDateRangeException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
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
class CreateProjectServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long ORG_UNIT_ID = 100L;
    private static final Long MANAGER_ID = 50L;

    @Mock
    private SaveProjectPort saveProjectPort;

    @Mock
    private LoadProjectPort loadProjectPort;

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

    private CreateProjectService service;

    @BeforeEach
    void setUp() {
        service = new CreateProjectService(
                saveProjectPort,
                loadProjectPort,
                loadOrgUnitPort,
                loadEmployeePort,
                loadUserPort,
                saveAuditLogPort,
                saveDeniedAuditLogPort,
                authorizationService
        );
    }

    @Test
    @DisplayName("TC-01: Tạo dự án thành công và tự động sinh mã")
    void testCreateProject_Success() {
        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));
        when(loadEmployeePort.findById(new EmployeeId(MANAGER_ID))).thenReturn(Optional.of(createEmployee(MANAGER_ID)));
        when(loadProjectPort.existsByProjectCode(any())).thenReturn(false);

        when(saveProjectPort.save(any(Project.class))).thenAnswer(invocation -> {
            Project input = invocation.getArgument(0);
            return new Project(
                    new ProjectId(1L),
                    input.getProjectCode(),
                    input.getProjectName(),
                    input.getOrgUnitId(),
                    input.getManagerId(),
                    input.getStartDate(),
                    input.getEndDate(),
                    input.getEstimatedHours(),
                    input.getDescription(),
                    ProjectStatus.ACTIVE,
                    input.getCreatedBy(),
                    input.getCreatedAt(),
                    null,
                    0L
            );
        });

        CreateProjectCommand command = new CreateProjectCommand(
                "Dự án Chuyển đổi số",
                ORG_UNIT_ID,
                MANAGER_ID,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(160),
                "Mô tả dự án số hóa"
        );

        ProjectResult result = service.createProject(command);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProjectName()).isEqualTo("Dự án Chuyển đổi số");
        assertThat(result.getProjectCode()).startsWith("PRJ-IT-");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(result.getEstimatedHours()).isEqualByComparingTo(BigDecimal.valueOf(160));

        verify(saveProjectPort).save(any(Project.class));
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Báo lỗi khi ngày kết thúc sớm hơn ngày bắt đầu")
    void testCreateProject_InvalidDateRange_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.ACTIVE)));

        CreateProjectCommand command = new CreateProjectCommand(
                "Dự án Lỗi Ngày",
                ORG_UNIT_ID,
                null,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 9, 1),
                BigDecimal.valueOf(100),
                null
        );

        assertThatThrownBy(() -> service.createProject(command))
                .isInstanceOf(InvalidProjectDateRangeException.class);
    }

    @Test
    @DisplayName("TC-03: Từ chối truy cập và ghi log khi ngoài phạm vi cây tổ chức (QTN-01)")
    void testCreateProject_OutsideOrgScope_ThrowsPermissionDenied() {
        User branchManager = createBranchManagerUser(200L);

        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(branchManager));
        when(loadOrgUnitPort.existsInOrgUnitBranch(ORG_UNIT_ID, 200L)).thenReturn(false);

        CreateProjectCommand command = new CreateProjectCommand(
                "Dự án Ngoài Phạm Vi",
                ORG_UNIT_ID,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(100),
                null
        );

        assertThatThrownBy(() -> service.createProject(command))
                .isInstanceOf(PermissionDeniedException.class);

        verify(saveDeniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Báo lỗi khi đơn vị tổ chức bị vô hiệu hóa (INACTIVE)")
    void testCreateProject_InactiveOrgUnit_ThrowsException() {
        when(authorizationService.require(PermissionCode.PROJECT_CREATE)).thenReturn(CURRENT_USER_ID);
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createAdminUser()));
        when(loadOrgUnitPort.findById(new OrgUnitId(ORG_UNIT_ID))).thenReturn(Optional.of(createOrgUnit(ORG_UNIT_ID, "IT", OrgUnitStatus.INACTIVE)));

        CreateProjectCommand command = new CreateProjectCommand(
                "Dự án Phòng Đã Đóng",
                ORG_UNIT_ID,
                null,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(100),
                null
        );

        assertThatThrownBy(() -> service.createProject(command))
                .isInstanceOf(InvalidProjectDataException.class)
                .hasMessageContaining("vô hiệu hóa");
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
                0L
        );
    }

    private User createBranchManagerUser(Long scopeOrgUnitId) {
        Role pmRole = new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án");
        return new User(
                new UserId(CURRENT_USER_ID),
                "pm_user",
                "hash",
                pmRole,
                UserStatus.ACTIVE,
                new EmployeeId(MANAGER_ID),
                DataScope.ORGANIZATION_BRANCH,
                scopeOrgUnitId,
                "pm@hrm.com",
                null,
                1,
                0L
        );
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
                null
        );
    }

    private Employee createEmployee(Long id) {
        return new Employee(
                new EmployeeId(id),
                new UserId(CURRENT_USER_ID),
                ORG_UNIT_ID,
                "EMP001",
                "Nguyen Van PM",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }
}
