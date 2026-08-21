package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.*;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private SaveUserPort saveUserPort;

    @Mock
    private LoadRolePort loadRolePort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private SaveEmployeePort saveEmployeePort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private UserService userService;

    private Role adminRole;
    private Role staffRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                loadUserPort,
                saveUserPort,
                loadRolePort,
                loadEmployeePort,
                saveEmployeePort,
                saveAuditLogPort,
                passwordEncoder
        );
        adminRole = new Role(new RoleId(1L), RoleCode.VT_06, "Quản trị viên");
        staffRole = new Role(new RoleId(2L), RoleCode.VT_04, "Nhân viên chuyên môn");
    }

    @Test
    @DisplayName("Tạo người dùng thành công với vai trò và hồ sơ nhân viên hợp lệ")
    void testCreateUser_Success() {
        CreateUserCommand command = new CreateUserCommand(
                "john_doe", "password123", "VT-04", "EMP-001", "John Doe", 10L
        );

        when(loadUserPort.existsByUsername("john_doe")).thenReturn(false);
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_hash");

        User savedUser = new User(new UserId(100L), "john_doe", "encoded_hash", staffRole, UserStatus.ACTIVE, null);
        when(saveUserPort.save(any(User.class))).thenReturn(savedUser);

        Employee savedEmployee = new Employee(new EmployeeId(200L), new UserId(100L), 10L, "EMP-001", "John Doe", false, 40, "ACTIVE");
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(savedEmployee);

        UserResult result = userService.createUser(command, 1L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("john_doe", result.getUsername());
        assertEquals("VT-04", result.getRoleCode());
        assertEquals(200L, result.getEmployeeId());
        assertEquals("John Doe", result.getFullName());

        verify(saveAuditLogPort, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Tạo người dùng thất bại khi Username đã tồn tại trong hệ thống")
    void testCreateUser_DuplicateUsername_ThrowsException() {
        CreateUserCommand command = new CreateUserCommand(
                "john_doe", "password123", "VT-04", "EMP-001", "John Doe", 10L
        );

        when(loadUserPort.existsByUsername("john_doe")).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> {
            userService.createUser(command, 1L);
        });

        verify(saveUserPort, never()).save(any());
        verify(saveEmployeePort, never()).save(any());
    }

    @Test
    @DisplayName("Khóa tài khoản thành công khi không vi phạm quy tắc an toàn")
    void testToggleUserStatus_Lock_Success() {
        User staffUser = new User(new UserId(2L), "staff", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(20L));

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(staffUser));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(staffUser);
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());

        UserResult result = userService.toggleUserStatus(2L, true, 1L);

        assertEquals(UserStatus.LOCKED, result.getStatus());
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Khóa tài khoản thất bại khi Admin tự khóa chính mình")
    void testToggleUserStatus_SelfLocking_ThrowsException() {
        User adminUser = new User(new UserId(1L), "admin", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(10L));

        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(adminUser));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);

        assertThrows(SelfLockingException.class, () -> {
            userService.toggleUserStatus(1L, true, 1L);
        });
    }

    @Test
    @DisplayName("Khóa tài khoản thất bại khi cố khóa Admin duy nhất còn lại")
    void testToggleUserStatus_LastAdmin_ThrowsException() {
        User adminUser = new User(new UserId(2L), "admin2", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(20L));

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(adminUser));
        when(loadUserPort.countActiveAdmins()).thenReturn(1L);

        assertThrows(LastAdminProtectionException.class, () -> {
            userService.toggleUserStatus(2L, true, 1L);
        });
    }

    @Test
    @DisplayName("Mở khóa tài khoản thành công")
    void testToggleUserStatus_Unlock_Success() {
        User staffUser = new User(new UserId(2L), "staff", "hash", staffRole, UserStatus.LOCKED, new EmployeeId(20L));

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(staffUser));
        when(saveUserPort.save(any(User.class))).thenReturn(staffUser);
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());

        UserResult result = userService.toggleUserStatus(2L, false, 1L);

        assertEquals(UserStatus.ACTIVE, result.getStatus());
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò và bộ phận thành công")
    void testUpdateUserRole_Success() {
        User user = new User(new UserId(2L), "user2", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(20L));
        Employee employee = new Employee(new EmployeeId(20L), new UserId(2L), 5L, "EMP-002", "User 2", false, 40, "ACTIVE");

        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-02", 15L);
        Role pmRole = new Role(new RoleId(3L), RoleCode.VT_02, "Quản lý dự án");

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_02)).thenReturn(Optional.of(pmRole));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(user);
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));

        UserResult result = userService.updateUserRole(command, 1L);

        assertEquals("VT-02", result.getRoleCode());
        assertEquals(15L, result.getDepartmentId());
        verify(saveEmployeePort, times(1)).save(employee);
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách người dùng với Batch Resolving không bị lỗi N+1 Query")
    void testGetUsers_PaginationAndBatchResolving() {
        User u1 = new User(new UserId(1L), "user1", "h1", staffRole, UserStatus.ACTIVE, new EmployeeId(10L));
        User u2 = new User(new UserId(2L), "user2", "h2", staffRole, UserStatus.ACTIVE, new EmployeeId(20L));

        when(loadUserPort.findAll(0, 20)).thenReturn(List.of(u1, u2));
        when(loadUserPort.count()).thenReturn(2L);

        Employee e1 = new Employee(new EmployeeId(10L), new UserId(1L), 5L, "E-1", "Employee 1", false, 40, "ACTIVE");
        Employee e2 = new Employee(new EmployeeId(20L), new UserId(2L), 5L, "E-2", "Employee 2", false, 40, "ACTIVE");
        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(1L), new UserId(2L)))).thenReturn(List.of(e1, e2));

        PageResult<UserResult> pageResult = userService.getUsers(0, 20);

        assertEquals(2, pageResult.getContent().size());
        assertEquals(2L, pageResult.getTotalElements());
        assertEquals(1, pageResult.getTotalPages());
        assertEquals("Employee 1", pageResult.getContent().get(0).getFullName());
        assertEquals("Employee 2", pageResult.getContent().get(1).getFullName());

        // Verify batch resolving method was called exactly once with list of user IDs
        verify(loadEmployeePort, times(1)).findAllByUserIdIn(List.of(new UserId(1L), new UserId(2L)));
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thành công")
    void testGetUserById_Success() {
        User user = new User(new UserId(5L), "user5", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(50L));
        Employee emp = new Employee(new EmployeeId(50L), new UserId(5L), 8L, "EMP-005", "User Five", false, 40, "ACTIVE");

        when(loadUserPort.findById(new UserId(5L))).thenReturn(Optional.of(user));
        when(loadEmployeePort.findByUserId(new UserId(5L))).thenReturn(Optional.of(emp));

        UserResult result = userService.getUserById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("User Five", result.getFullName());
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thất bại khi không tìm thấy")
    void testGetUserById_NotFound_ThrowsException() {
        when(loadUserPort.findById(new UserId(999L))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
    }
}
