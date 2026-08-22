package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.LastAdminProtectionException;
import com.hrm.employeemanagement.domain.exception.user.SelfLockingException;
import com.hrm.employeemanagement.domain.exception.user.UserAlreadyLockedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private LoadOrgUnitPort loadOrgUnitPort;

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
                loadOrgUnitPort,
                passwordEncoder
        );

        adminRole = new Role(new RoleId(1L), RoleCode.VT_06, "Quản trị hệ thống");
        staffRole = new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn");
    }

    @Test
    @DisplayName("Tạo người dùng và nhân viên thành công kèm resolve tên đơn vị tổ chức thực tế và ghi audit log")
    void testCreateUser_Success() {
        CreateUserCommand command = new CreateUserCommand(
                "john_doe", "password123", "VT-04", "EMP-001", "John Doe", 10L
        );

        when(loadUserPort.existsByUsername("john_doe")).thenReturn(false);
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");

        User createdUser = new User(new UserId(1L), "john_doe", "encoded_pass", staffRole, UserStatus.ACTIVE, null);
        when(saveUserPort.save(any(User.class))).thenReturn(createdUser);

        Employee createdEmployee = new Employee(new EmployeeId(100L), new UserId(1L), 10L, "EMP-001", "John Doe", false, 40, EmployeeStatus.ACTIVE);
        when(saveEmployeePort.save(any(Employee.class))).thenReturn(createdEmployee);

        OrgUnit orgUnit = activeOrgUnit(10L, "OU-10", "Phòng Kỹ thuật");
        when(loadOrgUnitPort.findById(new OrgUnitId(10L))).thenReturn(Optional.of(orgUnit));

        UserResult result = userService.createUser(command, 99L);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        assertEquals("VT-04", result.getRoleCode());
        assertEquals("John Doe", result.getFullName());
        assertEquals(10L, result.getOrgUnitId());
        assertEquals("Phòng Kỹ thuật", result.getOrgUnitName());

        verify(saveUserPort, times(1)).save(any(User.class));
        verify(saveEmployeePort, times(1)).save(any(Employee.class));
        verify(saveAuditLogPort, times(1)).save(any());
        verify(loadOrgUnitPort, times(1)).findById(new OrgUnitId(10L));
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
    @DisplayName("Khóa tài khoản Admin thành công và kích hoạt Pessimistic Lock trên Role VT-06")
    void testToggleUserStatus_LockAdmin_AcquiresPessimisticLockOnAdminRole() {
        User targetAdmin = new User(new UserId(2L), "admin2", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(20L));

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(targetAdmin));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(targetAdmin);
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());

        UserResult result = userService.toggleUserStatus(2L, true, 1L);

        assertEquals(UserStatus.LOCKED, result.getStatus());
        verify(loadRolePort, times(1)).lockRoleForUpdate(RoleCode.VT_06);
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
    @DisplayName("Khóa tài khoản thất bại khi tài khoản đã bị khóa trước đó (NCL-01-CN-002-TC-03)")
    void testToggleUserStatus_AlreadyLocked_ThrowsException() {
        User lockedUser = new User(new UserId(2L), "locked_staff", "hash", staffRole, UserStatus.LOCKED, new EmployeeId(20L));

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(lockedUser));

        assertThrows(UserAlreadyLockedException.class, () -> {
            userService.toggleUserStatus(2L, true, 1L);
        });

        verify(saveUserPort, never()).save(any());
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
    @DisplayName("Cập nhật vai trò và đơn vị tổ chức thành công kèm resolve orgUnitName")
    void testUpdateUserRole_Success() {
        User user = new User(new UserId(2L), "user2", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(20L));
        Employee employee = new Employee(new EmployeeId(20L), new UserId(2L), 5L, "EMP-002", "User 2", false, 40, EmployeeStatus.ACTIVE);

        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-02", 15L);
        Role pmRole = new Role(new RoleId(3L), RoleCode.VT_02, "Quản lý dự án");

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_02)).thenReturn(Optional.of(pmRole));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(user);
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.of(employee));

        OrgUnit orgUnit = activeOrgUnit(15L, "OU-15", "Ban Quản lý dự án");
        when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(orgUnit));

        UserResult result = userService.updateUserRole(command, 1L);

        assertEquals("VT-02", result.getRoleCode());
        assertEquals(15L, result.getOrgUnitId());
        assertEquals("Ban Quản lý dự án", result.getOrgUnitName());
        verify(saveEmployeePort, times(1)).save(employee);
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Hạ quyền Admin thành công và kích hoạt Pessimistic Lock trên Role VT-06")
    void testUpdateUserRole_DemoteAdmin_AcquiresPessimisticLockOnAdminRole() {
        User adminUser = new User(new UserId(2L), "admin2", "hash", adminRole, UserStatus.ACTIVE, new EmployeeId(20L));
        UpdateUserRoleCommand command = new UpdateUserRoleCommand(2L, "VT-04", 15L);

        when(loadUserPort.findById(new UserId(2L))).thenReturn(Optional.of(adminUser));
        when(loadRolePort.findByCode(RoleCode.VT_04)).thenReturn(Optional.of(staffRole));
        when(loadUserPort.countActiveAdmins()).thenReturn(2L);
        when(saveUserPort.save(any(User.class))).thenReturn(adminUser);
        when(loadEmployeePort.findByUserId(new UserId(2L))).thenReturn(Optional.empty());
        when(loadOrgUnitPort.findById(new OrgUnitId(15L))).thenReturn(Optional.of(activeOrgUnit(15L, "OU-15", "Ban Quản lý dự án")));

        UserResult result = userService.updateUserRole(command, 1L);

        assertEquals("VT-04", result.getRoleCode());
        verify(loadRolePort, times(1)).lockRoleForUpdate(RoleCode.VT_06);
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách người dùng với Batch Resolving Employee và OrgUnit tối ưu")
    void testGetUsers_PaginationAndBatchResolving() {
        User u1 = new User(new UserId(1L), "user1", "h1", staffRole, UserStatus.ACTIVE, new EmployeeId(10L));
        User u2 = new User(new UserId(2L), "user2", "h2", staffRole, UserStatus.ACTIVE, new EmployeeId(20L));

        when(loadUserPort.findAll(0, 20)).thenReturn(List.of(u1, u2));
        when(loadUserPort.count()).thenReturn(2L);

        Employee e1 = new Employee(new EmployeeId(10L), new UserId(1L), 5L, "E-1", "Employee 1", false, 40, EmployeeStatus.ACTIVE);
        Employee e2 = new Employee(new EmployeeId(20L), new UserId(2L), 5L, "E-2", "Employee 2", false, 40, EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(1L), new UserId(2L)))).thenReturn(List.of(e1, e2));

        OrgUnit orgUnit5 = activeOrgUnit(5L, "OU-05", "Phòng Nhân Sự");
        when(loadOrgUnitPort.findAllByIdIn(List.of(5L))).thenReturn(List.of(orgUnit5));

        PageResult<UserResult> pageResult = userService.getUsers(0, 20);

        assertEquals(2, pageResult.getContent().size());
        assertEquals(2L, pageResult.getTotalElements());
        assertEquals(1, pageResult.getTotalPages());
        assertEquals("Employee 1", pageResult.getContent().get(0).getFullName());
        assertEquals("Phòng Nhân Sự", pageResult.getContent().get(0).getOrgUnitName());
        assertEquals("Employee 2", pageResult.getContent().get(1).getFullName());
        assertEquals("Phòng Nhân Sự", pageResult.getContent().get(1).getOrgUnitName());

        verify(loadEmployeePort, times(1)).findAllByUserIdIn(List.of(new UserId(1L), new UserId(2L)));
        verify(loadOrgUnitPort, times(1)).findAllByIdIn(List.of(5L));
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thành công kèm resolve orgUnitName")
    void testGetUserById_Success() {
        User user = new User(new UserId(5L), "user5", "hash", staffRole, UserStatus.ACTIVE, new EmployeeId(50L));
        Employee emp = new Employee(new EmployeeId(50L), new UserId(5L), 8L, "EMP-005", "User Five", false, 40, EmployeeStatus.ACTIVE);
        OrgUnit orgUnit = activeOrgUnit(8L, "OU-08", "Ban Giám Đốc");

        when(loadUserPort.findById(new UserId(5L))).thenReturn(Optional.of(user));
        when(loadEmployeePort.findByUserId(new UserId(5L))).thenReturn(Optional.of(emp));
        when(loadOrgUnitPort.findById(new OrgUnitId(8L))).thenReturn(Optional.of(orgUnit));

        UserResult result = userService.getUserById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("User Five", result.getFullName());
        assertEquals("Ban Giám Đốc", result.getOrgUnitName());
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thất bại khi không tìm thấy")
    void testGetUserById_NotFound_ThrowsException() {
        when(loadUserPort.findById(new UserId(999L))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
    }

    private OrgUnit activeOrgUnit(Long id, String code, String name) {
        return new OrgUnit(
                new OrgUnitId(id),
                code,
                name,
                OrgUnitType.DEPARTMENT,
                null,
                "/" + id + "/",
                1,
                OrgUnitStatus.ACTIVE,
                null,
                null,
                null,
                null
        );
    }
}
