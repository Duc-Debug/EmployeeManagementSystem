package com.hrm.employeemanagement.application.service.user;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class UserServiceCreateTest extends BaseUserServiceTest {

    @Test
    @DisplayName("Tạo người dùng và nhân viên thành công kèm resolve tên đơn vị tổ chức thực tế và ghi audit log")
    void testCreateUser_Success() {
        when(authorizationService.require(PermissionCode.USER_CREATE))
                .thenReturn(ADMIN_ID);

        CreateUserCommand command = new CreateUserCommand(
                "john_doe",
                "password123",
                "VT-04",
                "EMP-001",
                "John Doe",
                10L
        );

        when(loadUserPort.existsByUsername("john_doe"))
                .thenReturn(false);

        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded_pass");

        User createdUser = new User(
                new UserId(1L),
                "john_doe",
                "encoded_pass",
                staffRole,
                UserStatus.ACTIVE,
                null
        );

        when(saveUserPort.save(any(User.class)))
                .thenReturn(createdUser);

        Employee createdEmployee = new Employee(
                new EmployeeId(100L),
                new UserId(1L),
                10L,
                "EMP-001",
                "John Doe",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(saveEmployeePort.save(any(Employee.class)))
                .thenReturn(createdEmployee);

        OrgUnit orgUnit = activeOrgUnit(
                10L,
                "OU-10",
                "Phòng Kỹ thuật"
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(10L)))
                .thenReturn(Optional.of(orgUnit));

        UserResult result = userService.createUser(command);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        assertEquals("VT-04", result.getRoleCode());
        assertEquals("John Doe", result.getFullName());
        assertEquals(10L, result.getOrgUnitId());
        assertEquals("Phòng Kỹ thuật", result.getOrgUnitName());

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_CREATE);

        verify(saveUserPort, times(1))
                .save(any(User.class));

        verify(saveEmployeePort, times(1))
                .save(any(Employee.class));

        verify(saveAuditLogPort, times(1))
                .save(any());

        verify(loadOrgUnitPort, times(1))
                .findById(new OrgUnitId(10L));
    }

    @Test
    @DisplayName("Tạo người dùng thất bại khi Username đã tồn tại trong hệ thống")
    void testCreateUser_DuplicateUsername_ThrowsException() {
        when(authorizationService.require(PermissionCode.USER_CREATE))
                .thenReturn(ADMIN_ID);

        CreateUserCommand command = new CreateUserCommand(
                "john_doe",
                "password123",
                "VT-04",
                "EMP-001",
                "John Doe",
                10L
        );

        when(loadUserPort.existsByUsername("john_doe"))
                .thenReturn(true);

        when(loadOrgUnitPort.findById(new OrgUnitId(10L)))
                .thenReturn(Optional.of(
                        activeOrgUnit(
                                10L,
                                "OU-10",
                                "Phòng Kỹ thuật"
                        )
                ));

        assertThrows(
                DuplicateUsernameException.class,
                () -> userService.createUser(command)
        );

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_CREATE);

        verify(saveUserPort, never())
                .save(any());

        verify(saveEmployeePort, never())
                .save(any());
    }

    @Test
    @DisplayName("Tạo người dùng bị từ chối khi orgUnit nằm ngoài ORGANIZATION_BRANCH scope")
    void testCreateUser_OrganizationBranchScopeOutsideOrgUnit_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_CREATE))
                .thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(
                DataScope.ORGANIZATION_BRANCH,
                5L
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        CreateUserCommand command = new CreateUserCommand(
                "john_doe",
                "password123",
                "VT-04",
                "EMP-001",
                "John Doe",
                20L
        );

        when(loadOrgUnitPort.existsInOrgUnitBranch(
                20L,
                5L
        )).thenReturn(false);

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.createUser(command)
        );

        verify(loadOrgUnitPort, never())
                .findById(new OrgUnitId(20L));

        verify(saveUserPort, never())
                .save(any());

        verify(saveEmployeePort, never())
                .save(any());
    }
}
