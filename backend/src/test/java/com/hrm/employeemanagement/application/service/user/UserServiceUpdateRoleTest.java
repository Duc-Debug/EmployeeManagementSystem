package com.hrm.employeemanagement.application.service.user;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class UserServiceUpdateRoleTest extends BaseUserServiceTest {

    private void stubUpdateRoleValidationBase(
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "user2",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));

        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());

        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(15L)))
                .thenReturn(Optional.of(
                        activeOrgUnit(
                                15L,
                                "OU-15",
                                "Ban Quản lý dự án"
                        )
                ));
    }

    @Test
    @DisplayName("Cập nhật phân quyền thành công và giữ nguyên OrgUnit của Employee")
    void testUpdateUserRole_Success() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "user2",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        Employee employee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                5L,
                "EMP-002",
                "User 2",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-02",
                        15L,
                        DataScope.SELF,
                        null
                );

        Role pmRole = new Role(
                new RoleId(3L),
                RoleCode.VT_02,
                "Quản lý dự án"
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));

        when(loadRolePort.findByCode(RoleCode.VT_02))
                .thenReturn(Optional.of(pmRole));

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(user);

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.of(employee));

        OrgUnit orgUnit = activeOrgUnit(
                5L,
                "OU-05",
                "Đơn vị hiện tại"
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(5L)))
                .thenReturn(Optional.of(orgUnit));

        UserResult result =
                userService.updateUserRole(command);

        assertEquals(
                "VT-02",
                result.getRoleCode()
        );

        assertEquals(
                5L,
                result.getOrgUnitId()
        );

        assertEquals(
                "Đơn vị hiện tại",
                result.getOrgUnitName()
        );

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_UPDATE_ROLE);

        verify(saveEmployeePort, never())
                .save(employee);

        ArgumentCaptor<AuditLog> updateAuthorizationAuditCaptor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(saveAuditLogPort, times(1))
                .save(updateAuthorizationAuditCaptor.capture());

        assertEquals(
                "UPDATE_AUTHORIZATION",
                updateAuthorizationAuditCaptor.getValue().getAction()
        );
    }

    @Test
    @DisplayName("Cập nhật authorization ghi audit actor, target, old/new và timestamp")
    void testUpdateUserRole_WritesAuthorizationChangeDetailsAudit() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "user2",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-02",
                        15L,
                        DataScope.ORGANIZATION_BRANCH,
                        5L
                );

        Role pmRole = new Role(
                new RoleId(2L),
                RoleCode.VT_02,
                "Quản lý dự án"
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));

        when(loadRolePort.findByCode(RoleCode.VT_02))
                .thenReturn(Optional.of(pmRole));

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());

        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(15L)))
                .thenReturn(
                        Optional.of(
                                activeOrgUnit(
                                        15L,
                                        "OU-15",
                                        "Ban Quản lý dự án"
                                )
                        )
                );

        when(loadOrgUnitPort.findById(new OrgUnitId(5L)))
                .thenReturn(
                        Optional.of(
                                activeOrgUnit(
                                        5L,
                                        "OU-05",
                                        "Khối Công nghệ"
                                )
                        )
                );

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(user);

        userService.updateUserRole(command);

        ArgumentCaptor<AuditLog> auditCaptor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(saveAuditLogPort, times(1))
                .save(auditCaptor.capture());

        AuditLog auditLog = auditCaptor.getValue();

        assertEquals(ADMIN_ID, auditLog.getUserId());
        assertEquals("UPDATE_AUTHORIZATION", auditLog.getAction());
        assertEquals("users", auditLog.getTableName());
        assertEquals(2L, auditLog.getRecordId());
        assertEquals("role=VT-04;dataScope=SELF;scopeOrgUnitId=null", auditLog.getOldValue());
        assertEquals("role=VT-02;dataScope=ORGANIZATION_BRANCH;scopeOrgUnitId=5", auditLog.getNewValue());
        assertNotNull(auditLog.getCreatedAt());
    }

    @Test
    @DisplayName("Hạ quyền Admin thành công và kích hoạt Pessimistic Lock trên Role VT-06")
    void testUpdateUserRole_DemoteAdmin_AcquiresPessimisticLockOnAdminRole() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
        )).thenReturn(ADMIN_ID);

        User adminUser = new User(
                new UserId(2L),
                "admin2",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.SELF,
                        null
                );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(adminUser));

        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(adminUser);

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());

        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(15L)))
                .thenReturn(
                        Optional.of(
                                activeOrgUnit(
                                        15L,
                                        "OU-15",
                                        "Ban Quản lý dự án"
                                )
                        )
                );

        UserResult result =
                userService.updateUserRole(command);

        assertEquals("VT-04", result.getRoleCode());

        verify(loadRolePort, times(1))
                .lockRoleForUpdate(RoleCode.VT_06);

        verify(saveAuditLogPort, times(1))
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò áp dụng DataScope SELF với scopeOrgUnitId null")
    void testUpdateUserRole_AppliesSelfDataScope() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);

        User user = testUser(2L, staffRole, 20L);
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.SELF, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());
        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Ban Quản lý dự án")));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(user);

        UserResult result = userService.updateUserRole(command);

        assertNotNull(result);
        assertEquals(DataScope.SELF, user.getDataScope());
        assertEquals(DataScope.SELF, result.getDataScope());
        assertNull(user.getScopeOrgUnitId());
    }

    @Test
    @DisplayName("Cập nhật vai trò áp dụng DataScope COMPANY với scopeOrgUnitId null")
    void testUpdateUserRole_AppliesCompanyDataScope() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);

        User user = testUser(2L, staffRole, 20L);
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());
        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Ban Quản lý dự án")));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(user);

        UserResult result = userService.updateUserRole(command);

        assertNotNull(result);
        assertEquals(DataScope.COMPANY, user.getDataScope());
        assertEquals(DataScope.COMPANY, result.getDataScope());
        assertNull(user.getScopeOrgUnitId());
    }

    @Test
    @DisplayName("Cập nhật vai trò áp dụng DataScope ORGANIZATION_BRANCH với scopeOrgUnitId hợp lệ")
    void testUpdateUserRole_AppliesOrganizationBranchDataScope() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);

        User user = testUser(2L, staffRole, 20L);
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.ORGANIZATION_BRANCH, 5L);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());
        lenient().when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Ban Quản lý dự án")));
        when(loadOrgUnitPort.findById(new OrgUnitId(5L))).thenReturn(Optional.of(activeOrgUnit(5L, "OU-05", "Khối Công nghệ")));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(user);

        UserResult result = userService.updateUserRole(command);

        assertNotNull(result);
        assertEquals(DataScope.ORGANIZATION_BRANCH, result.getDataScope());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject SELF khi truyền scopeOrgUnitId")
    void testUpdateUserRole_SelfScopeWithOrgUnitId_Rejects() {
        stubUpdateRoleValidationBase(DataScope.SELF, 5L);
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.SELF, 5L);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserRole(command));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject COMPANY khi truyền scopeOrgUnitId")
    void testUpdateUserRole_CompanyScopeWithOrgUnitId_Rejects() {
        stubUpdateRoleValidationBase(DataScope.COMPANY, 5L);
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.COMPANY, 5L);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserRole(command));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject ORGANIZATION_BRANCH khi scopeOrgUnitId null")
    void testUpdateUserRole_OrganizationBranchScopeWithNullOrgUnitId_Rejects() {
        stubUpdateRoleValidationBase(DataScope.ORGANIZATION_BRANCH, null);
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.ORGANIZATION_BRANCH, null);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserRole(command));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject ORGANIZATION_BRANCH khi scope orgUnit không tồn tại")
    void testUpdateUserRole_OrganizationBranchScopeWithNonexistentOrgUnit_Rejects() {
        stubUpdateRoleValidationBase(DataScope.ORGANIZATION_BRANCH, 5L);
        when(loadOrgUnitPort.findById(new OrgUnitId(5L))).thenReturn(Optional.empty());

        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.ORGANIZATION_BRANCH, 5L);

        assertThrows(OrgUnitNotFoundException.class, () -> userService.updateUserRole(command));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject ORGANIZATION_BRANCH khi scope orgUnit không hoạt động")
    void testUpdateUserRole_OrganizationBranchScopeWithInactiveOrgUnit_Rejects() {
        stubUpdateRoleValidationBase(DataScope.ORGANIZATION_BRANCH, 5L);
        when(loadOrgUnitPort.findById(new OrgUnitId(5L))).thenReturn(Optional.of(orgUnit(5L, "OU-05", "Khối Công nghệ", OrgUnitStatus.INACTIVE)));

        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.ORGANIZATION_BRANCH, 5L);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUserRole(command));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò bị từ chối khi target user ngoài ORGANIZATION_BRANCH scope")
    void testUpdateUserRole_OrganizationBranchScopeOutsideTarget_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);
        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 5L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.existsInOrgUnitBranch(20L, 5L)).thenReturn(false);

        UpdateUserRoleCommand command = new UpdateUserRoleCommand(20L, "VT-04", 15L, DataScope.SELF, null);

        assertThrows(PermissionDeniedException.class, () -> userService.updateUserRole(command));
        verify(loadUserPort, never()).findById(new UserId(20L));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò bỏ qua legacy orgUnitId ngoài scope")
    void testUpdateUserRole_IgnoresLegacyOrgUnitIdOutsideScope() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);
        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 5L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.existsInOrgUnitBranch(2L, 5L)).thenReturn(true);

        User user = new User(new UserId(2L), "target", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(20L));
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 20L, DataScope.SELF, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(user)).thenReturn(user);

        UserResult result = userService.updateUserRole(command);

        assertEquals("VT-04", result.getRoleCode());
        verify(loadOrgUnitPort, never()).existsInOrgUnitBranch(20L, 5L);
        verify(saveUserPort).save(user);
    }

    @Test
    @DisplayName("Cập nhật vai trò bị từ chối khi actor branch gán COMPANY data scope")
    void testUpdateUserRole_OrganizationBranchScopeAssignsCompany_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);
        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 5L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.existsInOrgUnitBranch(2L, 5L)).thenReturn(true);
        lenient().when(loadOrgUnitPort.existsInOrgUnitBranch(15L, 5L)).thenReturn(true);

        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L, DataScope.COMPANY, null);

        assertThrows(PermissionDeniedException.class, () -> userService.updateUserRole(command));
        verify(loadUserPort, never()).findById(new UserId(2L));
        verify(saveUserPort, never()).save(any(User.class));
        verify(saveAuditLogPort, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò giữ nguyên OrgUnit của Employee")
    void testUpdateUserRole_DoesNotChangeEmployeeOrgUnit() {
        when(authorizationService.require(PermissionCode.USER_UPDATE_ROLE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        Employee employee = testEmployee(20L, 2L, 15L, "EMP-002");
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 25L, DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));
        when(saveUserPort.save(user)).thenReturn(user);
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Phòng hiện tại")));

        UserResult result = userService.updateUserRole(command);

        assertEquals(15L, employee.getOrgUnitId());
        assertEquals("Phòng hiện tại", result.getOrgUnitName());
        verify(saveEmployeePort, never()).save(employee);
    }
}
