package com.hrm.employeemanagement.application.service.user;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class UserServiceLockUnlockTest extends BaseUserServiceTest {

    @Test
    @DisplayName("Khóa tài khoản thành công khi không vi phạm quy tắc an toàn")
    void testToggleUserStatus_Lock_Success() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(ADMIN_ID);

        User staffUser = new User(
                new UserId(2L),
                "staff",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(staffUser));

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(staffUser);

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());

        UserResult result =
                userService.toggleUserStatus(2L, true);

        assertEquals(
                UserStatus.LOCKED,
                result.getStatus()
        );

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_TOGGLE_STATUS);

        ArgumentCaptor<AuditLog> auditCaptor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(saveAuditLogPort, times(1))
                .save(auditCaptor.capture());

        assertEquals(
                "LOCK_USER",
                auditCaptor.getValue().getAction()
        );
    }

    @Test
    @DisplayName("Khóa tài khoản Admin thành công và kích hoạt Pessimistic Lock trên Role VT-06")
    void testToggleUserStatus_LockAdmin_AcquiresPessimisticLockOnAdminRole() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(ADMIN_ID);

        User targetAdmin = new User(
                new UserId(2L),
                "admin2",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(targetAdmin));

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(targetAdmin);

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());

        UserResult result =
                userService.toggleUserStatus(2L, true);

        assertEquals(
                UserStatus.LOCKED,
                result.getStatus()
        );

        verify(loadRolePort, times(1))
                .lockRoleForUpdate(RoleCode.VT_06);

        ArgumentCaptor<AuditLog> auditCaptor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(saveAuditLogPort, times(1))
                .save(auditCaptor.capture());

        assertEquals(
                "LOCK_USER",
                auditCaptor.getValue().getAction()
        );
    }

    @Test
    @DisplayName("Khóa tài khoản thất bại khi Admin tự khóa chính mình")
    void testToggleUserStatus_SelfLocking_ThrowsException() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(1L);

        User adminUser = new User(
                new UserId(1L),
                "admin",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        when(loadUserPort.findById(new UserId(1L)))
                .thenReturn(Optional.of(adminUser));

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        assertThrows(
                SelfLockingException.class,
                () -> userService.toggleUserStatus(
                        1L,
                        true
                )
        );

        verify(saveUserPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Khóa tài khoản thất bại khi cố khóa Admin duy nhất còn lại")
    void testToggleUserStatus_LastAdmin_ThrowsException() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(ADMIN_ID);

        User adminUser = new User(
                new UserId(2L),
                "admin2",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(adminUser));

        when(loadUserPort.countActiveAdmins())
                .thenReturn(1L);

        assertThrows(
                LastAdminProtectionException.class,
                () -> userService.toggleUserStatus(
                        2L,
                        true
                )
        );

        verify(saveUserPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Khóa tài khoản thất bại khi tài khoản đã bị khóa trước đó (NCL-01-CN-002-TC-03)")
    void testToggleUserStatus_AlreadyLocked_ThrowsException() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(ADMIN_ID);

        User lockedUser = new User(
                new UserId(2L),
                "locked_staff",
                "hash",
                staffRole,
                UserStatus.LOCKED,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(lockedUser));

        assertThrows(
                UserAlreadyLockedException.class,
                () -> userService.toggleUserStatus(
                        2L,
                        true
                )
        );

        verify(saveUserPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Mở khóa tài khoản thành công")
    void testToggleUserStatus_Unlock_Success() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(ADMIN_ID);

        User staffUser = new User(
                new UserId(2L),
                "staff",
                "hash",
                staffRole,
                UserStatus.LOCKED,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(staffUser));

        when(saveUserPort.save(any(User.class)))
                .thenReturn(staffUser);

        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());

        UserResult result =
                userService.toggleUserStatus(2L, false);

        assertEquals(
                UserStatus.ACTIVE,
                result.getStatus()
        );

        ArgumentCaptor<AuditLog> auditCaptor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(saveAuditLogPort, times(1))
                .save(auditCaptor.capture());

        assertEquals(
                "UNLOCK_USER",
                auditCaptor.getValue().getAction()
        );
    }

    @Test
    @DisplayName("Khóa tài khoản bị từ chối khi target user ngoài ORGANIZATION_BRANCH scope")
    void testToggleUserStatus_OrganizationBranchScopeOutsideTarget_ThrowsPermissionDeniedException() {
        when(authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        )).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(
                DataScope.ORGANIZATION_BRANCH,
                5L
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.existsInOrgUnitBranch(
                20L,
                5L
        )).thenReturn(false);

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.toggleUserStatus(
                        20L,
                        true
                )
        );

        verify(loadUserPort, never())
                .findById(new UserId(20L));

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }
}
