package com.hrm.employeemanagement.domain.model.user;

import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private Role adminRole;
    private Role staffRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(1L, RoleCode.VT_06, "Quản trị viên");
        staffRole = new Role(2L, RoleCode.VT_04, "Nhân viên chuyên môn");
    }

    @Test
    @DisplayName("Admin không thể tự khóa chính mình")
    void testAdminCannotLockSelf() {
        User adminUser = new User(1L, "admin", "hash", adminRole, UserStatus.ACTIVE, 1L);

        assertThrows(SelfLockingException.class, () -> {
            adminUser.lock(1L, 2);
        });
    }

    @Test
    @DisplayName("Không thể khóa Quản trị viên duy nhất trong hệ thống")
    void testCannotLockLastAdmin() {
        User adminUser = new User(2L, "admin2", "hash", adminRole, UserStatus.ACTIVE, 2L);

        assertThrows(LastAdminProtectionException.class, () -> {
            adminUser.lock(1L, 1); // 1L is current logged-in admin, adminUser has id 2L, but activeAdminCount is 1
        });
    }

    @Test
    @DisplayName("Khóa tài khoản thành công khi có nhiều hơn 1 Quản trị viên active")
    void testLockUserSuccess() {
        User staffUser = new User(3L, "staff", "hash", staffRole, UserStatus.ACTIVE, 3L);

        staffUser.lock(1L, 2);

        assertEquals(UserStatus.LOCKED, staffUser.getStatus());
        assertFalse(staffUser.isActive());
    }

    @Test
    @DisplayName("Mở lại tài khoản thành công")
    void testUnlockUserSuccess() {
        User staffUser = new User(3L, "staff", "hash", staffRole, UserStatus.LOCKED, 3L);

        staffUser.unlock();

        assertEquals(UserStatus.ACTIVE, staffUser.getStatus());
        assertTrue(staffUser.isActive());
    }

    @Test
    @DisplayName("Không thể hạ quyền Admin nếu là Quản trị viên duy nhất")
    void testCannotChangeRoleOfLastAdmin() {
        User adminUser = new User(2L, "admin2", "hash", adminRole, UserStatus.ACTIVE, 2L);

        assertThrows(LastAdminProtectionException.class, () -> {
            adminUser.changeRole(staffRole, 1);
        });
    }
}
