package com.hrm.employeemanagement.application.service.user;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.user.UpdateUserCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.DuplicateEmployeeCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

class UserServiceUpdateTest extends BaseUserServiceTest {

    @Test
    @DisplayName("updateUser giữ nguyên orgUnitId của Employee khi command.orgUnitId() là null")
    void testUpdateUser_NullOrgUnitId_PreservesEmployeeOrgUnit() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        Employee employee = testEmployee(20L, 2L, 15L, "EMP-002");
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe Updated", "john.updated@company.com", "EMP-002-UPDATED", null, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));
        when(saveUserPort.save(user)).thenReturn(user);
        when(saveEmployeePort.save(employee)).thenReturn(employee);
        when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Phòng hiện tại")));

        UserResult result = userService.updateUser(command);

        assertEquals(15L, employee.getOrgUnitId(), "orgUnitId của Employee phải được bảo toàn khi command.orgUnitId() là null");
        assertEquals(15L, result.getOrgUnitId(), "result.orgUnitId phải phản ánh đúng orgUnitId hiện tại");
        assertEquals("John Doe Updated", employee.getFullName());
        assertEquals("EMP-002-UPDATED", employee.getEmployeeCode());
        assertEquals("Phòng hiện tại", result.getOrgUnitName());
        verify(saveEmployeePort).save(employee);
    }

    @Test
    @DisplayName("updateUser cập nhật orgUnitId mới cho Employee khi orgUnitId truyền vào có giá trị hợp lệ")
    void testUpdateUser_NonNullOrgUnitId_UpdatesEmployeeOrgUnit() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        Employee employee = testEmployee(20L, 2L, 15L, "EMP-002");
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe Updated", "john.updated@company.com", "EMP-002-UPDATED", 25L, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));
        when(saveUserPort.save(user)).thenReturn(user);
        when(saveEmployeePort.save(employee)).thenReturn(employee);
        when(loadOrgUnitPort.findById(new OrgUnitId(25L))).thenReturn(Optional.of(activeOrgUnit(25L, "OU-25", "Phòng ban mới")));

        UserResult result = userService.updateUser(command);

        assertEquals(25L, employee.getOrgUnitId());
        assertEquals(25L, result.getOrgUnitId());
        assertEquals("John Doe Updated", employee.getFullName());
        assertEquals("Phòng ban mới", result.getOrgUnitName());
        verify(saveEmployeePort).save(employee);
    }

    @Test
    @DisplayName("updateUser ném DuplicateEmployeeCodeException khi mã nhân viên bị trùng với nhân viên khác")
    void testUpdateUser_DuplicateEmployeeCode_ThrowsException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        Employee employee = testEmployee(20L, 2L, 15L, "EMP-002");
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe Updated", "john.updated@company.com", "EMP-EXISTS", null, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));
        when(saveUserPort.save(user)).thenReturn(user);
        when(loadEmployeePort.existsByEmployeeCodeAndIdNot("EMP-EXISTS", new EmployeeId(20L))).thenReturn(true);

        assertThrows(DuplicateEmployeeCodeException.class, () -> userService.updateUser(command));
        verify(saveEmployeePort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser không kiểm tra trùng nếu employeeCode được giữ nguyên như cũ")
    void testUpdateUser_SameEmployeeCode_DoesNotQueryDuplicate() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        Employee employee = testEmployee(20L, 2L, 15L, "EMP-002");
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe Updated", "john.updated@company.com", "EMP-002", null, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));
        when(saveUserPort.save(user)).thenReturn(user);
        when(saveEmployeePort.save(employee)).thenReturn(employee);
        when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Phòng hiện tại")));

        UserResult result = userService.updateUser(command);

        assertEquals("EMP-002", result.getFullName() != null ? employee.getEmployeeCode() : null);
        verify(loadEmployeePort, never()).existsByEmployeeCodeAndIdNot(any(), any());
        verify(saveEmployeePort).save(employee);
    }

    @Test
    @DisplayName("updateUser ném RoleNotFoundException khi roleCode không tồn tại trong database thay vì tự động tạo mới")
    void testUpdateUser_RoleNotFound_ThrowsRoleNotFoundException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe Updated", "john.updated@company.com", "EMP-002", null, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> userService.updateUser(command));
        verify(loadRolePort, never()).save(any());
        verify(saveUserPort, never()).save(any());
        verify(saveEmployeePort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser ném DuplicateUsernameException khi email bị trùng với tài khoản khác")
    void testUpdateUser_DuplicateEmail_ThrowsDuplicateUsernameException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe", "duplicate@company.com", null, null, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadUserPort.existsByEmail("duplicate@company.com")).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> userService.updateUser(command));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser ném PermissionDeniedException khi target user nằm ngoài data scope của admin")
    void testUpdateUser_TargetUserOutsideDataScope_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User branchAdmin = testUser(ADMIN_ID, staffRole, 10L);
        branchAdmin.changeDataScope(DataScope.ORGANIZATION_BRANCH, 10L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(branchAdmin));
        when(loadUserPort.existsInOrgUnitBranch(99L, 10L)).thenReturn(false);

        UpdateUserCommand command = new UpdateUserCommand(99L, "Outside User", "outside@company.com", null, null, "VT-04", DataScope.SELF, null);

        assertThrows(PermissionDeniedException.class, () -> userService.updateUser(command));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser ném PermissionDeniedException khi OrgUnit được chọn nằm ngoài data scope của admin")
    void testUpdateUser_OrgUnitOutsideDataScope_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User branchAdmin = testUser(ADMIN_ID, staffRole, 10L);
        branchAdmin.changeDataScope(DataScope.ORGANIZATION_BRANCH, 10L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(branchAdmin));
        when(loadUserPort.existsInOrgUnitBranch(2L, 10L)).thenReturn(true);
        when(loadOrgUnitPort.existsInOrgUnitBranch(99L, 10L)).thenReturn(false);

        UpdateUserCommand command = new UpdateUserCommand(2L, "User 2", "user2@company.com", null, 99L, "VT-04", DataScope.SELF, null);

        assertThrows(PermissionDeniedException.class, () -> userService.updateUser(command));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser ném OrgUnitNotFoundException khi gán dataScope ORGANIZATION_BRANCH nhưng scopeOrgUnitId không tồn tại")
    void testUpdateUser_OrganizationBranch_ScopeOrgUnitNotFound_ThrowsOrgUnitNotFoundException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User user = testUser(2L, staffRole, 20L);
        UpdateUserCommand command = new UpdateUserCommand(2L, "John Doe", "john@company.com", null, null, "VT-04", DataScope.ORGANIZATION_BRANCH, 999L);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadOrgUnitPort.findById(new OrgUnitId(999L))).thenReturn(Optional.empty());

        assertThrows(OrgUnitNotFoundException.class, () -> userService.updateUser(command));
        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser ném LastAdminProtectionException khi cố gắng hạ quyền Quản trị viên duy nhất của hệ thống")
    void testUpdateUser_DowngradeLastAdmin_ThrowsLastAdminProtectionException() {
        when(authorizationService.require(PermissionCode.USER_UPDATE)).thenReturn(ADMIN_ID);
        User systemAdminUser = testUser(2L, adminRole, 20L);
        UpdateUserCommand command = new UpdateUserCommand(2L, "Admin Target", "admin_target@company.com", null, null, "VT-04", DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(systemAdminUser));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadUserPort.countActiveAdmins()).thenReturn(1L);

        assertThrows(LastAdminProtectionException.class, () -> userService.updateUser(command));
        verify(loadRolePort).lockRoleForUpdate(RoleCode.VT_06);
        verify(saveUserPort, never()).save(any());
    }
}
