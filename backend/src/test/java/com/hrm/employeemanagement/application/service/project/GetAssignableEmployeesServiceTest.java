package com.hrm.employeemanagement.application.service.project;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class GetAssignableEmployeesServiceTest {

    private static final Long CURRENT_USER_ID = 99L;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private AuthorizationService authorizationService;

    private GetAssignableEmployeesService service;

    private User createCompanyUser(Long userId) {
        return new User(
                new UserId(userId),
                "director_user",
                "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban Giám đốc"),
                UserStatus.ACTIVE,
                null,
                DataScope.COMPANY,
                null,
                1L
        );
    }

    @BeforeEach
    void setUp() {
        service = new GetAssignableEmployeesService(loadEmployeePort, loadOrgUnitPort, loadUserPort, authorizationService);
        org.mockito.Mockito.lenient().when(authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE)).thenReturn(CURRENT_USER_ID);
        org.mockito.Mockito.lenient().when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(createCompanyUser(CURRENT_USER_ID)));
    }

    @Test
    @DisplayName("Loại trừ Quản trị viên hệ thống (VT-06) khỏi danh sách giao việc")
    void shouldExcludeSystemAdmin() {
        Employee adminEmp = new Employee(
                new EmployeeId(1L), new UserId(1L), 10L, "ADMIN", "Administrator",
                "IT", LocalDate.of(2020, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee regularEmp = new Employee(
                new EmployeeId(2L), new UserId(2L), 10L, "EMP01", "Nguyen Van A",
                "Developer", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findAllActive()).thenReturn(List.of(adminEmp, regularEmp));

        User adminUser = new User(
                new UserId(1L), "admin", "hash",
                new Role(new RoleId(6L), RoleCode.VT_06, "Quản trị viên"),
                UserStatus.ACTIVE, new EmployeeId(1L)
        );
        User regularUser = new User(
                new UserId(2L), "employee01", "hash",
                new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE, new EmployeeId(2L)
        );

        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(adminUser));
        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(regularUser));

        OrgUnit ou = new OrgUnit(
                new OrgUnitId(10L), "TECH", "Phòng Kỹ thuật",
                com.hrm.employeemanagement.domain.orgunit.OrgUnitType.DEPARTMENT,
                null, "/10/", 1, com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus.ACTIVE,
                "Phòng Kỹ thuật", null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
        );
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(ou));

        List<ProjectMemberResult> results = service.getAssignableEmployees();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).employeeId()).isEqualTo(2L);
        assertThat(results.get(0).fullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    @DisplayName("Ẩn nhân sự có ngày hết hạn hợp đồng trước ngày bắt đầu công việc được xét")
    void shouldFilterOutEmployeesWithContractEndedBeforeStartDate() {
        LocalDate taskStartDate = LocalDate.of(2026, 7, 1);

        Employee expiredEmp = new Employee(
                new EmployeeId(2L), new UserId(2L), 10L, "EMP01", "Nguyen Van HetHan",
                "Developer", LocalDate.of(2021, 1, 1), LocalDate.of(2026, 6, 30),
                false, 40, EmployeeStatus.ACTIVE
        );
        Employee activeContractEmp = new Employee(
                new EmployeeId(3L), new UserId(3L), 10L, "EMP02", "Tran Thi ConHan",
                "Tester", LocalDate.of(2021, 1, 1), LocalDate.of(2026, 12, 31),
                false, 40, EmployeeStatus.ACTIVE
        );
        Employee permanentEmp = new Employee(
                new EmployeeId(4L), new UserId(4L), 10L, "EMP03", "Le Van VoHan",
                "Designer", LocalDate.of(2021, 1, 1), null,
                false, 40, EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findAllActive()).thenReturn(List.of(expiredEmp, activeContractEmp, permanentEmp));

        User user2 = new User(new UserId(2L), "u2", "hash", new Role(new RoleId(4L), RoleCode.VT_04, "Dev"), UserStatus.ACTIVE, new EmployeeId(2L));
        User user3 = new User(new UserId(3L), "u3", "hash", new Role(new RoleId(4L), RoleCode.VT_04, "Tester"), UserStatus.ACTIVE, new EmployeeId(3L));
        User user4 = new User(new UserId(4L), "u4", "hash", new Role(new RoleId(4L), RoleCode.VT_04, "Designer"), UserStatus.ACTIVE, new EmployeeId(4L));

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user2));
        when(loadUserPort.findById(new UserId(3L))).thenReturn(Optional.of(user3));
        when(loadUserPort.findById(new UserId(4L))).thenReturn(Optional.of(user4));

        List<ProjectMemberResult> results = service.getAssignableEmployees(taskStartDate);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ProjectMemberResult::employeeId).containsExactlyInAnyOrder(3L, 4L);
        assertThat(results).extracting(ProjectMemberResult::contractEndDate).contains(LocalDate.of(2026, 12, 31), (LocalDate) null);
    }

    @Test
    @DisplayName("Lọc nhân sự theo nhánh phòng ban khi người dùng có scope ORGANIZATION_BRANCH")
    void shouldFilterEmployeesByBranchScopeWhenScopeIsOrganizationBranch() {
        Long branchOrgUnitId = 10L;
        User branchUser = new User(
                new UserId(CURRENT_USER_ID), "resource_manager", "hash",
                new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE, null, DataScope.ORGANIZATION_BRANCH, branchOrgUnitId, 1L
        );
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(branchUser));

        Employee empInsideBranch = new Employee(
                new EmployeeId(10L), new UserId(10L), 10L, "EMP10", "Nguyen Van Trong Nhanh",
                "Dev", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee empOutsideBranch = new Employee(
                new EmployeeId(20L), new UserId(20L), 99L, "EMP20", "Tran Van Ngoai Nhanh",
                "Dev", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findAllActive()).thenReturn(List.of(empInsideBranch, empOutsideBranch));

        User user10 = new User(new UserId(10L), "u10", "hash", new Role(new RoleId(4L), RoleCode.VT_04, "Dev"), UserStatus.ACTIVE, new EmployeeId(10L));
        User user20 = new User(new UserId(20L), "u20", "hash", new Role(new RoleId(4L), RoleCode.VT_04, "Dev"), UserStatus.ACTIVE, new EmployeeId(20L));
        when(loadUserPort.findById(new UserId(10L))).thenReturn(Optional.of(user10));
        when(loadUserPort.findById(new UserId(20L))).thenReturn(Optional.of(user20));

        when(loadOrgUnitPort.existsInOrgUnitBranch(10L, branchOrgUnitId)).thenReturn(true);
        when(loadOrgUnitPort.existsInOrgUnitBranch(99L, branchOrgUnitId)).thenReturn(false);

        List<ProjectMemberResult> results = service.getAssignableEmployees();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).employeeId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Chỉ trả về chính bản thân khi người dùng có scope SELF")
    void shouldFilterEmployeesBySelfScopeWhenScopeIsSelf() {
        User selfUser = new User(
                new UserId(CURRENT_USER_ID), "pm_self", "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "PM"),
                UserStatus.ACTIVE, null, DataScope.SELF, null, 1L
        );
        when(loadUserPort.findById(new UserId(CURRENT_USER_ID))).thenReturn(Optional.of(selfUser));

        Employee currentEmp = new Employee(
                new EmployeeId(CURRENT_USER_ID), new UserId(CURRENT_USER_ID), 10L, "EMP_ME", "Toi La PM",
                "PM", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        Employee otherEmp = new Employee(
                new EmployeeId(88L), new UserId(88L), 10L, "EMP88", "Nguoi Khac",
                "Dev", LocalDate.of(2021, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findAllActive()).thenReturn(List.of(currentEmp, otherEmp));

        List<ProjectMemberResult> results = service.getAssignableEmployees();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).employeeId()).isEqualTo(CURRENT_USER_ID);
    }
}
