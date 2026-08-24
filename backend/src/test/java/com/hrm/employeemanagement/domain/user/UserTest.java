package com.hrm.employeemanagement.domain.user;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyActiveException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private Role adminRole;
    private Role staffRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(new RoleId(1L), RoleCode.VT_06, "Quản trị viên");
        staffRole = new Role(new RoleId(2L), RoleCode.VT_04, "Nhân viên chuyên môn");
    }

    @Test
    @DisplayName("Admin không thể tự khóa chính mình")
    void testAdminCannotLockSelf() {
        User adminUser = new User(new UserId(1L), "admin", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(1L));

        assertThrows(SelfLockingException.class, () -> {
            adminUser.lock(new UserId(1L), 2);
        });
    }

    @Test
    @DisplayName("Không thể khóa Quản trị viên duy nhất trong hệ thống")
    void testCannotLockLastAdmin() {
        User adminUser = new User(new UserId(2L), "admin2", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(2L));

        assertThrows(LastAdminProtectionException.class, () -> {
            adminUser.lock(new UserId(1L), 1);
        });
    }

    @Test
    @DisplayName("Báo lỗi khi khóa tài khoản đã bị khóa trước đó (NCL-01-CN-002-TC-03)")
    void testLockAlreadyLockedUser_ThrowsException() {
        User staffUser = new User(new UserId(3L), "staff", "hash", staffRole, UserStatus.LOCKED, new EmployeeId(3L));

        assertThrows(UserAlreadyLockedException.class, () -> {
            staffUser.lock(new UserId(1L), 2);
        });
    }

    @Test
    @DisplayName("Khóa tài khoản thành công khi không vi phạm quy tắc an toàn")
    void testLockUserSuccess() {
        User staffUser = new User(new UserId(3L), "staff", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(3L));

        staffUser.lock(new UserId(1L), 2);

        assertEquals(UserStatus.LOCKED, staffUser.getStatus());
        assertFalse(staffUser.isActive());
    }

    @Test
    @DisplayName("Báo lỗi khi mở khóa tài khoản đang ở trạng thái hoạt động")
    void testUnlockAlreadyActiveUser_ThrowsException() {
        User staffUser = new User(new UserId(3L), "staff", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(3L));

        assertThrows(UserAlreadyActiveException.class, () -> {
            staffUser.unlock();
        });
    }

    @Test
    @DisplayName("Mở lại tài khoản thành công khi đang bị khóa")
    void testUnlockUserSuccess() {
        User staffUser = new User(new UserId(3L), "staff", "hash", staffRole, UserStatus.LOCKED, new EmployeeId(3L));

        staffUser.unlock();

        assertEquals(UserStatus.ACTIVE, staffUser.getStatus());
        assertTrue(staffUser.isActive());
    }

    @Test
    @DisplayName("Không thể hạ quyền Admin nếu là Quản trị viên duy nhất")
    void testCannotChangeRoleOfLastAdmin() {
        User adminUser = new User(new UserId(2L), "admin2", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(2L));

        assertThrows(LastAdminProtectionException.class, () -> {
            adminUser.changeRole(staffRole, 1);
        });
    }

    @Test
    @DisplayName("Tạo mới System Admin mặc định có DataScope COMPANY")
    void testCreateSystemAdmin_DefaultsToCompanyDataScope() {
        User adminUser = User.createNew(
                "admin",
                "hash",
                adminRole,
                null
        );

        assertEquals(DataScope.COMPANY, adminUser.getDataScope());
        assertNull(adminUser.getScopeOrgUnitId());
    }

    @Test
    @DisplayName("Domain reject System Admin với DataScope SELF")
    void testSystemAdminWithSelfScope_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new User(
                        new UserId(4L),
                        "admin_self",
                        "hash",
                        adminRole,
                        UserStatus.ACTIVE,
                        null,
                        DataScope.SELF,
                        null,
                        null
                )
        );
    }

    @Test
    @DisplayName("Domain reject khi đổi authorization thành System Admin nhưng scope không phải COMPANY")
    void testChangeAuthorizationToSystemAdminWithSelfScope_ThrowsException() {
        User staffUser = User.createNew(
                "staff",
                "hash",
                staffRole,
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> staffUser.changeAuthorization(
                        adminRole,
                        DataScope.SELF,
                        null,
                        2
                )
        );
    }
}
