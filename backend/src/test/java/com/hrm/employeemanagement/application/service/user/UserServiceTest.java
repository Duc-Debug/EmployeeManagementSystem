package com.hrm.employeemanagement.application.service.user;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserCommand;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.DuplicateEmployeeCodeException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ADMIN_ID = 1L;

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
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private AuthorizationService authorizationService;

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
                deniedAuditLogPort,
                loadOrgUnitPort,
                passwordEncoder,
                authorizationService
        );

        adminRole = new Role(
                new RoleId(1L),
                RoleCode.VT_06,
                "Quản trị hệ thống"
        );

        staffRole = new Role(
                new RoleId(4L),
                RoleCode.VT_04,
                "Nhân viên chuyên môn"
        );

        User currentAdmin = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentAdmin.changeDataScope(
                DataScope.COMPANY,
                null
        );

        lenient().when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentAdmin));
    }

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
        /*
         * Authenticated actor ID = 1.
         * Target user ID = 1.
         * Đây chính là điều kiện self-lock.
         */
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

        AuditLog auditLog =
                auditCaptor.getValue();

        assertEquals(
                ADMIN_ID,
                auditLog.getUserId()
        );

        assertEquals(
                "UPDATE_AUTHORIZATION",
                auditLog.getAction()
        );

        assertEquals(
                "users",
                auditLog.getTableName()
        );

        assertEquals(
                2L,
                auditLog.getRecordId()
        );

        assertEquals(
                "role=VT-04;dataScope=SELF;scopeOrgUnitId=null",
                auditLog.getOldValue()
        );

        assertEquals(
                "role=VT-02;dataScope=ORGANIZATION_BRANCH;scopeOrgUnitId=5",
                auditLog.getNewValue()
        );

        assertNotNull(
                auditLog.getCreatedAt()
        );
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

        assertEquals(
                "VT-04",
                result.getRoleCode()
        );

        verify(loadRolePort, times(1))
                .lockRoleForUpdate(RoleCode.VT_06);

        verify(saveAuditLogPort, times(1))
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò áp dụng DataScope SELF với scopeOrgUnitId null")
    void testUpdateUserRole_AppliesSelfDataScope() {
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
                        "VT-04",
                        15L,
                        DataScope.SELF,
                        null
                );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));

        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));

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

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(user);

        UserResult result =
                userService.updateUserRole(command);

        assertNotNull(result);

        assertEquals(
                DataScope.SELF,
                user.getDataScope()
        );

        assertEquals(
                DataScope.SELF,
                result.getDataScope()
        );

        assertNull(
                user.getScopeOrgUnitId()
        );
    }

    @Test
    @DisplayName("Cập nhật vai trò áp dụng DataScope COMPANY với scopeOrgUnitId null")
    void testUpdateUserRole_AppliesCompanyDataScope() {
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
                        "VT-04",
                        15L,
                        DataScope.COMPANY,
                        null
                );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));

        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));

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

        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);

        when(saveUserPort.save(any(User.class)))
                .thenReturn(user);

        UserResult result =
                userService.updateUserRole(command);

        assertNotNull(result);

        assertEquals(
                DataScope.COMPANY,
                user.getDataScope()
        );

        assertEquals(
                DataScope.COMPANY,
                result.getDataScope()
        );

        assertNull(
                user.getScopeOrgUnitId()
        );
    }

    @Test
    @DisplayName("Cập nhật vai trò áp dụng DataScope ORGANIZATION_BRANCH với scopeOrgUnitId hợp lệ")
    void testUpdateUserRole_AppliesOrganizationBranchDataScope() {
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
                        "VT-04",
                        15L,
                        DataScope.ORGANIZATION_BRANCH,
                        5L
                );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));

        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));

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

        UserResult result =
                userService.updateUserRole(command);

        assertNotNull(result);

        assertEquals(
                DataScope.ORGANIZATION_BRANCH,
                user.getDataScope()
        );

        assertEquals(
                DataScope.ORGANIZATION_BRANCH,
                result.getDataScope()
        );

        assertEquals(
                5L,
                user.getScopeOrgUnitId()
        );

        assertEquals(
                5L,
                result.getScopeOrgUnitId()
        );
    }

    @Test
    @DisplayName("Cập nhật vai trò reject SELF khi truyền scopeOrgUnitId")
    void testUpdateUserRole_SelfScopeWithOrgUnitId_Rejects() {
        stubUpdateRoleValidationBase(
                DataScope.SELF,
                5L
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.SELF,
                        5L
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserRole(command)
        );

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject COMPANY khi truyền scopeOrgUnitId")
    void testUpdateUserRole_CompanyScopeWithOrgUnitId_Rejects() {
        stubUpdateRoleValidationBase(
                DataScope.COMPANY,
                5L
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.COMPANY,
                        5L
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserRole(command)
        );

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject ORGANIZATION_BRANCH khi scopeOrgUnitId null")
    void testUpdateUserRole_OrganizationBranchScopeWithNullOrgUnitId_Rejects() {
        stubUpdateRoleValidationBase(
                DataScope.ORGANIZATION_BRANCH,
                null
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.ORGANIZATION_BRANCH,
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserRole(command)
        );

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject ORGANIZATION_BRANCH khi scope orgUnit không tồn tại")
    void testUpdateUserRole_OrganizationBranchScopeWithNonexistentOrgUnit_Rejects() {
        stubUpdateRoleValidationBase(
                DataScope.ORGANIZATION_BRANCH,
                5L
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(5L)))
                .thenReturn(Optional.empty());

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.ORGANIZATION_BRANCH,
                        5L
                );

        assertThrows(
                OrgUnitNotFoundException.class,
                () -> userService.updateUserRole(command)
        );

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò reject ORGANIZATION_BRANCH khi scope orgUnit không hoạt động")
    void testUpdateUserRole_OrganizationBranchScopeWithInactiveOrgUnit_Rejects() {
        stubUpdateRoleValidationBase(
                DataScope.ORGANIZATION_BRANCH,
                5L
        );

        when(loadOrgUnitPort.findById(new OrgUnitId(5L)))
                .thenReturn(Optional.of(
                        orgUnit(
                                5L,
                                "OU-05",
                                "Khối Công nghệ",
                                OrgUnitStatus.INACTIVE
                        )
                ));

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.ORGANIZATION_BRANCH,
                        5L
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserRole(command)
        );

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò bị từ chối khi target user ngoài ORGANIZATION_BRANCH scope")
    void testUpdateUserRole_OrganizationBranchScopeOutsideTarget_ThrowsPermissionDeniedException() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
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

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        20L,
                        "VT-04",
                        15L,
                        DataScope.SELF,
                        null
                );

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.updateUserRole(command)
        );

        verify(loadUserPort, never())
                .findById(new UserId(20L));

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Cập nhật vai trò bỏ qua legacy orgUnitId ngoài scope")
    void testUpdateUserRole_IgnoresLegacyOrgUnitIdOutsideScope() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
        )).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(
                DataScope.ORGANIZATION_BRANCH,
                5L
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.existsInOrgUnitBranch(
                2L,
                5L
        )).thenReturn(true);

        User user = new User(
                new UserId(2L),
                "target",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        20L,
                        DataScope.SELF,
                        null
                );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.empty());
        when(loadUserPort.countActiveAdmins())
                .thenReturn(2L);
        when(saveUserPort.save(user))
                .thenReturn(user);

        UserResult result =
                userService.updateUserRole(command);

        assertEquals(
                "VT-04",
                result.getRoleCode()
        );

        verify(loadOrgUnitPort, never())
                .existsInOrgUnitBranch(
                        20L,
                        5L
                );

        verify(saveUserPort)
                .save(user);
    }

    @Test
    @DisplayName("Cập nhật vai trò bị từ chối khi actor branch gán COMPANY data scope")
    void testUpdateUserRole_OrganizationBranchScopeAssignsCompany_ThrowsPermissionDeniedException() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE_ROLE
        )).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(
                DataScope.ORGANIZATION_BRANCH,
                5L
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.existsInOrgUnitBranch(
                2L,
                5L
        )).thenReturn(true);

        lenient().when(loadOrgUnitPort.existsInOrgUnitBranch(
                15L,
                5L
        )).thenReturn(true);

        UpdateUserRoleCommand command =
                new UpdateUserRoleCommand(
                        2L,
                        "VT-04",
                        15L,
                        DataScope.COMPANY,
                        null
                );

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.updateUserRole(command)
        );

        verify(loadUserPort, never())
                .findById(new UserId(2L));

        verify(saveUserPort, never())
                .save(any(User.class));

        verify(saveAuditLogPort, never())
                .save(any());
    }

    @Test
    @DisplayName("Lấy danh sách người dùng với Batch Resolving Employee và OrgUnit tối ưu")
    void testGetUsers_PaginationAndBatchResolving() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User u1 = new User(
                new UserId(1L),
                "user1",
                "h1",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );
        u1.changeDataScope(
                DataScope.COMPANY,
                null
        );

        User u2 = new User(
                new UserId(2L),
                "user2",
                "h2",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(u1));

        when(loadUserPort.findAll(0, 20))
                .thenReturn(List.of(u1, u2));

        when(loadUserPort.count())
                .thenReturn(2L);

        Employee e1 = new Employee(
                new EmployeeId(10L),
                new UserId(1L),
                5L,
                "E-1",
                "Employee 1",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        Employee e2 = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                5L,
                "E-2",
                "Employee 2",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(
                loadEmployeePort.findAllByUserIdIn(
                        List.of(
                                new UserId(1L),
                                new UserId(2L)
                        )
                )
        ).thenReturn(List.of(e1, e2));

        OrgUnit orgUnit5 = activeOrgUnit(
                5L,
                "OU-05",
                "Phòng Nhân Sự"
        );

        when(loadOrgUnitPort.findAllByIdIn(List.of(5L)))
                .thenReturn(List.of(orgUnit5));

        PageResult<UserResult> pageResult =
                userService.getUsers(0, 20);

        assertEquals(
                2,
                pageResult.getContent().size()
        );

        assertEquals(
                2L,
                pageResult.getTotalElements()
        );

        assertEquals(
                1,
                pageResult.getTotalPages()
        );

        assertEquals(
                "Employee 1",
                pageResult.getContent()
                        .get(0)
                        .getFullName()
        );

        assertEquals(
                "Phòng Nhân Sự",
                pageResult.getContent()
                        .get(0)
                        .getOrgUnitName()
        );

        assertEquals(
                "Employee 2",
                pageResult.getContent()
                        .get(1)
                        .getFullName()
        );

        assertEquals(
                "Phòng Nhân Sự",
                pageResult.getContent()
                        .get(1)
                        .getOrgUnitName()
        );

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_READ);

        verify(loadEmployeePort, times(1))
                .findAllByUserIdIn(
                        List.of(
                                new UserId(1L),
                                new UserId(2L)
                        )
                );

        verify(loadOrgUnitPort, times(1))
                .findAllByIdIn(List.of(5L));
    }

    @Test
    @DisplayName("Lấy danh sách người dùng theo DataScope ORGANIZATION_BRANCH")
    void testGetUsers_OrganizationBranchScope() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "branch_manager",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L),
                DataScope.ORGANIZATION_BRANCH,
                5L,
                null
        );

        User scopedUser = new User(
                new UserId(2L),
                "user2",
                "h2",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.findByOrgUnitBranch(5L, 0, 20))
                .thenReturn(List.of(scopedUser));

        when(loadUserPort.countByOrgUnitBranch(5L))
                .thenReturn(1L);

        Employee employee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                8L,
                "E-2",
                "Employee 2",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(
                loadEmployeePort.findAllByUserIdIn(
                        List.of(new UserId(2L))
                )
        ).thenReturn(List.of(employee));

        when(loadOrgUnitPort.findAllByIdIn(List.of(8L)))
                .thenReturn(
                        List.of(
                                activeOrgUnit(
                                        8L,
                                        "OU-08",
                                        "Nhánh Công nghệ"
                                )
                        )
                );

        PageResult<UserResult> pageResult =
                userService.getUsers(0, 20);

        assertEquals(
                1,
                pageResult.getContent().size()
        );

        assertEquals(
                1L,
                pageResult.getTotalElements()
        );

        assertEquals(
                "Employee 2",
                pageResult.getContent()
                        .get(0)
                        .getFullName()
        );

        assertEquals(
                "Nhánh Công nghệ",
                pageResult.getContent()
                        .get(0)
                        .getOrgUnitName()
        );

        verify(loadUserPort, times(1))
                .findByOrgUnitBranch(5L, 0, 20);

        verify(loadUserPort, times(1))
                .countByOrgUnitBranch(5L);
    }

    @Test
    @DisplayName("Lấy danh sách người dùng theo DataScope SELF chỉ trả về current user ở trang đầu")
    void testGetUsers_SelfScope() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "self_user",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L),
                DataScope.SELF,
                null,
                null
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        Employee employee = new Employee(
                new EmployeeId(10L),
                new UserId(ADMIN_ID),
                5L,
                "E-1",
                "Self User",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(
                loadEmployeePort.findAllByUserIdIn(
                        List.of(new UserId(ADMIN_ID))
                )
        ).thenReturn(List.of(employee));

        when(loadOrgUnitPort.findAllByIdIn(List.of(5L)))
                .thenReturn(
                        List.of(
                                activeOrgUnit(
                                        5L,
                                        "OU-05",
                                        "Phòng Nhân Sự"
                                )
                        )
                );

        PageResult<UserResult> pageResult =
                userService.getUsers(0, 20);

        assertEquals(
                1,
                pageResult.getContent().size()
        );

        assertEquals(
                1L,
                pageResult.getTotalElements()
        );

        assertEquals(
                "Self User",
                pageResult.getContent()
                        .get(0)
                        .getFullName()
        );
    }

    @Test
    @DisplayName("getUsers áp dụng DataScope mới ở request kế tiếp khi scope bị shrink từ COMPANY sang SELF")
    void testGetUsers_ScopeShrinkCompanyToSelf_AppliesOnNextRequest() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(
                ADMIN_ID,
                ADMIN_ID
        );

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.COMPANY,
                null
        );

        User otherUser = new User(
                new UserId(2L),
                "user2",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.findAll(0, 20))
                .thenReturn(List.of(currentUser, otherUser));

        when(loadUserPort.count())
                .thenReturn(2L);

        Employee currentEmployee = new Employee(
                new EmployeeId(10L),
                new UserId(ADMIN_ID),
                5L,
                "E-1",
                "Admin",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        Employee otherEmployee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                6L,
                "E-2",
                "User Two",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(loadEmployeePort.findAllByUserIdIn(
                List.of(
                        new UserId(ADMIN_ID),
                        new UserId(2L)
                )
        )).thenReturn(
                List.of(
                        currentEmployee,
                        otherEmployee
                )
        );

        when(loadEmployeePort.findAllByUserIdIn(
                List.of(new UserId(ADMIN_ID))
        )).thenReturn(List.of(currentEmployee));

        when(loadOrgUnitPort.findAllByIdIn(List.of(5L, 6L)))
                .thenReturn(
                        List.of(
                                activeOrgUnit(
                                        5L,
                                        "OU-05",
                                        "Khối A"
                                ),
                                activeOrgUnit(
                                        6L,
                                        "OU-06",
                                        "Khối B"
                                )
                        )
                );

        when(loadOrgUnitPort.findAllByIdIn(List.of(5L)))
                .thenReturn(
                        List.of(
                                activeOrgUnit(
                                        5L,
                                        "OU-05",
                                        "Khối A"
                                )
                        )
                );

        PageResult<UserResult> firstRequest =
                userService.getUsers(0, 20);

        assertEquals(
                2,
                firstRequest.getContent().size()
        );

        assertEquals(
                2L,
                firstRequest.getTotalElements()
        );

        currentUser.changeDataScope(
                DataScope.SELF,
                null
        );

        PageResult<UserResult> secondRequest =
                userService.getUsers(0, 20);

        assertEquals(
                1,
                secondRequest.getContent().size()
        );

        assertEquals(
                1L,
                secondRequest.getTotalElements()
        );

        assertEquals(
                ADMIN_ID,
                secondRequest.getContent()
                        .get(0)
                        .getId()
        );

        verify(authorizationService, times(2))
                .require(PermissionCode.USER_READ);

        verify(loadUserPort, times(1))
                .findAll(0, 20);

        verify(loadUserPort, times(1))
                .count();
    }

    @Test
    @DisplayName("COMPANY đọc user khác thành công kèm resolve orgUnitName")
    void testGetUserById_CompanyScope_ReadsOtherUser() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                adminRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.COMPANY,
                null
        );

        User targetUser = new User(
                new UserId(5L),
                "user5",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(50L)
        );

        Employee employee = new Employee(
                new EmployeeId(50L),
                new UserId(5L),
                8L,
                "EMP-005",
                "User Five",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        OrgUnit orgUnit = activeOrgUnit(
                8L,
                "OU-08",
                "Ban Giám Đốc"
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.findById(new UserId(5L)))
                .thenReturn(Optional.of(targetUser));

        when(loadEmployeePort.findByUserId(new UserId(5L)))
                .thenReturn(Optional.of(employee));

        when(loadOrgUnitPort.findById(new OrgUnitId(8L)))
                .thenReturn(Optional.of(orgUnit));

        UserResult result =
                userService.getUserById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("User Five", result.getFullName());
        assertEquals(
                "Ban Giám Đốc",
                result.getOrgUnitName()
        );

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_READ);
    }

    @Test
    @DisplayName("SELF đọc chính mình thành công")
    void testGetUserById_SelfScope_ReadsSelf() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "self_user",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.SELF,
                null
        );

        Employee employee = new Employee(
                new EmployeeId(10L),
                new UserId(ADMIN_ID),
                8L,
                "EMP-001",
                "Self User",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadEmployeePort.findByUserId(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(employee));

        when(loadOrgUnitPort.findById(new OrgUnitId(8L)))
                .thenReturn(Optional.of(
                        activeOrgUnit(
                                8L,
                                "OU-08",
                                "Phòng Cá nhân"
                        )
                ));

        UserResult result =
                userService.getUserById(ADMIN_ID);

        assertNotNull(result);
        assertEquals(ADMIN_ID, result.getId());
        assertEquals("Self User", result.getFullName());
        assertEquals("Phòng Cá nhân", result.getOrgUnitName());

        verify(loadUserPort, never())
                .existsInOrgUnitBranch(
                        any(),
                        any()
                );
    }

    @Test
    @DisplayName("SELF đọc user khác bị từ chối trước khi load target")
    void testGetUserById_SelfScope_ReadsOtherUser_ThrowsPermissionDeniedException() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "self_user",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.SELF,
                null
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.getUserById(5L)
        );

        verify(loadUserPort, never())
                .findById(new UserId(5L));

        verify(loadEmployeePort, never())
                .findByUserId(any());

        ArgumentCaptor<AuditLog> auditCaptor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(deniedAuditLogPort)
                .save(auditCaptor.capture());

        assertEquals(
                ADMIN_ID,
                auditCaptor.getValue().getUserId()
        );
        assertEquals(
                "USER_ACCESS_DENIED",
                auditCaptor.getValue().getAction()
        );
        assertEquals(
                "users",
                auditCaptor.getValue().getTableName()
        );
        assertEquals(
                5L,
                auditCaptor.getValue().getRecordId()
        );
        assertEquals(
                "permission=USER_READ;dataScope=SELF;scopeOrgUnitId=null;reason=OUTSIDE_DATA_SCOPE",
                auditCaptor.getValue().getNewValue()
        );
    }

    @Test
    @DisplayName("ORGANIZATION_BRANCH đọc user trong nhánh thành công")
    void testGetUserById_OrganizationBranchScope_ReadsUserInsideBranch() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "manager",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.ORGANIZATION_BRANCH,
                10L
        );

        User targetUser = new User(
                new UserId(5L),
                "branch_user",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(50L)
        );

        Employee employee = new Employee(
                new EmployeeId(50L),
                new UserId(5L),
                12L,
                "EMP-005",
                "Branch User",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.existsInOrgUnitBranch(
                5L,
                10L
        )).thenReturn(true);

        when(loadUserPort.findById(new UserId(5L)))
                .thenReturn(Optional.of(targetUser));

        when(loadEmployeePort.findByUserId(new UserId(5L)))
                .thenReturn(Optional.of(employee));

        when(loadOrgUnitPort.findById(new OrgUnitId(12L)))
                .thenReturn(Optional.of(
                        activeOrgUnit(
                                12L,
                                "OU-12",
                                "Team Backend"
                        )
                ));

        UserResult result =
                userService.getUserById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Branch User", result.getFullName());
        assertEquals("Team Backend", result.getOrgUnitName());

        verify(loadUserPort, times(1))
                .existsInOrgUnitBranch(
                        5L,
                        10L
                );
    }

    @Test
    @DisplayName("ORGANIZATION_BRANCH đọc user ngoài nhánh bị từ chối trước khi load target")
    void testGetUserById_OrganizationBranchScope_ReadsUserOutsideBranch_ThrowsPermissionDeniedException() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "manager",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.ORGANIZATION_BRANCH,
                10L
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.existsInOrgUnitBranch(
                5L,
                10L
        )).thenReturn(false);

        assertThrows(
                PermissionDeniedException.class,
                () -> userService.getUserById(5L)
        );

        verify(loadUserPort, times(1))
                .existsInOrgUnitBranch(
                        5L,
                        10L
                );

        verify(loadUserPort, never())
                .findById(new UserId(5L));

        verify(loadEmployeePort, never())
                .findByUserId(any());
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thất bại khi không tìm thấy")
    void testGetUserById_NotFound_ThrowsException() {
        when(authorizationService.require(
                PermissionCode.USER_READ
        )).thenReturn(ADMIN_ID);

        User currentUser = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                DataScope.COMPANY,
                null
        );

        when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadUserPort.findById(new UserId(999L)))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(999L)
        );

        verify(authorizationService, times(1))
                .require(PermissionCode.USER_READ);
    }

    private OrgUnit activeOrgUnit(
            Long id,
            String code,
            String name
    ) {
        return orgUnit(
                id,
                code,
                name,
                OrgUnitStatus.ACTIVE
        );
    }

    private OrgUnit orgUnit(
            Long id,
            String code,
            String name,
            OrgUnitStatus status
    ) {
        return new OrgUnit(
                new OrgUnitId(id),
                code,
                name,
                OrgUnitType.DEPARTMENT,
                null,
                "/" + id + "/",
                1,
                status,
                null,
                null,
                null,
                null
        );
    }

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

    private User currentUserWithScope(
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        User currentUser = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                dataScope,
                scopeOrgUnitId
        );

        return currentUser;
    }
    @Test
@DisplayName("getUsers với COMPANY scope trả về toàn bộ user")
void testGetUsers_CompanyScope_ReturnsAllUsers() {
    when(
            authorizationService.require(
                    PermissionCode.USER_READ
            )
    ).thenReturn(1L);

    User currentUser = new User(
            new UserId(1L),
            "admin",
            "hash",
            adminRole,
            UserStatus.ACTIVE,
            new EmployeeId(10L)
    );

    currentUser.changeDataScope(
            DataScope.COMPANY,
            null
    );

    when(
            loadUserPort.findById(
                    new UserId(1L)
            )
    ).thenReturn(Optional.of(currentUser));

    User u1 = currentUser;

    User u2 = new User(
            new UserId(2L),
            "user2",
            "hash",
            staffRole,
            UserStatus.ACTIVE,
            new EmployeeId(20L)
    );

    when(
            loadUserPort.findAll(0, 20)
    ).thenReturn(List.of(u1, u2));

    when(
            loadUserPort.count()
    ).thenReturn(2L);

    Employee e1 = new Employee(
            new EmployeeId(10L),
            new UserId(1L),
            5L,
            "E-1",
            "Admin",
            false,
            40,
            EmployeeStatus.ACTIVE
    );

    Employee e2 = new Employee(
            new EmployeeId(20L),
            new UserId(2L),
            6L,
            "E-2",
            "User Two",
            false,
            40,
            EmployeeStatus.ACTIVE
    );

    when(
            loadEmployeePort.findAllByUserIdIn(
                    List.of(
                            new UserId(1L),
                            new UserId(2L)
                    )
            )
    ).thenReturn(List.of(e1, e2));

    when(
            loadOrgUnitPort.findAllByIdIn(
                    List.of(5L, 6L)
            )
    ).thenReturn(List.of(
            activeOrgUnit(5L, "OU-05", "Khối A"),
            activeOrgUnit(6L, "OU-06", "Khối B")
    ));

    PageResult<UserResult> result =
            userService.getUsers(0, 20);

    assertEquals(
            2,
            result.getContent().size()
    );

    assertEquals(
            2L,
            result.getTotalElements()
    );

    verify(loadUserPort, times(1))
            .findAll(0, 20);

    verify(loadUserPort, times(1))
            .count();

    verify(loadUserPort, never())
            .findByOrgUnitBranch(
                    any(),
                    any(Integer.class),
                    any(Integer.class)
            );
}
@Test
@DisplayName("getUsers với SELF scope chỉ trả về chính user hiện tại")
void testGetUsers_SelfScope_ReturnsOnlyCurrentUser() {
    when(
            authorizationService.require(
                    PermissionCode.USER_READ
            )
    ).thenReturn(1L);

    User currentUser = new User(
            new UserId(1L),
            "staff",
            "hash",
            staffRole,
            UserStatus.ACTIVE,
            new EmployeeId(10L)
    );

    currentUser.changeDataScope(
            DataScope.SELF,
            null
    );

    when(
            loadUserPort.findById(
                    new UserId(1L)
            )
    ).thenReturn(Optional.of(currentUser));

    Employee employee = new Employee(
            new EmployeeId(10L),
            new UserId(1L),
            5L,
            "E-1",
            "Current User",
            false,
            40,
            EmployeeStatus.ACTIVE
    );

    when(
            loadEmployeePort.findAllByUserIdIn(
                    List.of(new UserId(1L))
            )
    ).thenReturn(List.of(employee));

    when(
            loadOrgUnitPort.findAllByIdIn(
                    List.of(5L)
            )
    ).thenReturn(
            List.of(
                    activeOrgUnit(
                            5L,
                            "OU-05",
                            "Phòng Kỹ thuật"
                    )
            )
    );

    PageResult<UserResult> result =
            userService.getUsers(0, 20);

    assertEquals(
            1,
            result.getContent().size()
    );

    assertEquals(
            1L,
            result.getTotalElements()
    );

    assertEquals(
            1L,
            result.getContent().get(0).getId()
    );

    verify(loadUserPort, never())
            .findAll(
                    any(Integer.class),
                    any(Integer.class)
            );

    verify(loadUserPort, never())
            .findByOrgUnitBranch(
                    any(),
                    any(Integer.class),
                    any(Integer.class)
            );
}
@Test
@DisplayName("getUsers với ORGANIZATION_BRANCH chỉ trả user thuộc nhánh được cấp")
void testGetUsers_OrganizationBranchScope_ReturnsBranchUsers() {
    when(
            authorizationService.require(
                    PermissionCode.USER_READ
            )
    ).thenReturn(1L);

    User currentUser = new User(
            new UserId(1L),
            "manager",
            "hash",
            staffRole,
            UserStatus.ACTIVE,
            new EmployeeId(10L)
    );

    currentUser.changeDataScope(
            DataScope.ORGANIZATION_BRANCH,
            5L
    );

    when(
            loadUserPort.findById(
                    new UserId(1L)
            )
    ).thenReturn(Optional.of(currentUser));

    User branchUser = new User(
            new UserId(2L),
            "branch_user",
            "hash",
            staffRole,
            UserStatus.ACTIVE,
            new EmployeeId(20L)
    );

    when(
            loadUserPort.findByOrgUnitBranch(
                    5L,
                    0,
                    20
            )
    ).thenReturn(
            List.of(branchUser)
    );

    when(
            loadUserPort.countByOrgUnitBranch(
                    5L
            )
    ).thenReturn(1L);

    Employee branchEmployee = new Employee(
            new EmployeeId(20L),
            new UserId(2L),
            8L,
            "E-2",
            "Branch User",
            false,
            40,
            EmployeeStatus.ACTIVE
    );

    when(
            loadEmployeePort.findAllByUserIdIn(
                    List.of(new UserId(2L))
            )
    ).thenReturn(
            List.of(branchEmployee)
    );

    when(
            loadOrgUnitPort.findAllByIdIn(
                    List.of(8L)
            )
    ).thenReturn(
            List.of(
                    activeOrgUnit(
                            8L,
                            "OU-08",
                            "Team Backend"
                    )
            )
    );

    PageResult<UserResult> result =
            userService.getUsers(0, 20);

    assertEquals(
            1,
            result.getContent().size()
    );

    assertEquals(
            1L,
            result.getTotalElements()
    );

    assertEquals(
            2L,
            result.getContent().get(0).getId()
    );

    verify(loadUserPort, times(1))
            .findByOrgUnitBranch(
                    5L,
                    0,
                    20
            );

    verify(loadUserPort, times(1))
            .countByOrgUnitBranch(
                    5L
            );

    verify(loadUserPort, never())
            .findAll(
                    any(Integer.class),
                    any(Integer.class)
            );
}

@Test
@DisplayName("updateUserRole không điều chuyển OrgUnit của Employee")
void testUpdateUserRole_DoesNotChangeEmployeeOrgUnit() {
    when(authorizationService.require(
            PermissionCode.USER_UPDATE_ROLE
    )).thenReturn(ADMIN_ID);

    User user = new User(
            new UserId(2L),
            "john_doe",
            "hash",
            staffRole,
            UserStatus.ACTIVE,
            new EmployeeId(20L)
    );

    Employee employee = new Employee(
            new EmployeeId(20L),
            new UserId(2L),
            15L,
            "EMP-002",
            "John Doe",
            false,
            40,
            EmployeeStatus.ACTIVE
    );

    UpdateUserRoleCommand command =
            new UpdateUserRoleCommand(
                    2L,
                    "VT-04",
                    99L,
                    DataScope.COMPANY,
                    null
            );

    when(loadUserPort.findById(new UserId(2L)))
            .thenReturn(Optional.of(user));
    when(loadRolePort.findByCode(RoleCode.VT_04))
            .thenReturn(Optional.of(staffRole));
    when(loadEmployeePort.findByUserId(new UserId(2L)))
            .thenReturn(Optional.of(employee));
    when(loadUserPort.countActiveAdmins())
            .thenReturn(2L);
    when(saveUserPort.save(user))
            .thenReturn(user);
    when(loadOrgUnitPort.findById(new OrgUnitId(15L)))
            .thenReturn(
                    Optional.of(
                            activeOrgUnit(
                                    15L,
                                    "OU-15",
                                    "Phòng hiện tại"
                            )
                    )
            );

    UserResult result =
            userService.updateUserRole(command);

    assertEquals(
            15L,
            employee.getOrgUnitId()
    );
    assertEquals(
            15L,
            result.getOrgUnitId()
    );

    verify(saveEmployeePort, never())
            .save(any());
    verify(loadOrgUnitPort, never())
            .findById(new OrgUnitId(99L));
}

    @Test
    @DisplayName("updateUser không làm mất orgUnitId hiện tại của Employee khi orgUnitId truyền vào là null")
    void testUpdateUser_NullOrgUnitId_PreservesEmployeeOrgUnit() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "john_doe",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        Employee employee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                15L,
                "EMP-002",
                "John Doe",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        UpdateUserCommand command = new UpdateUserCommand(
                2L,
                "John Doe Updated",
                "john.updated@company.com",
                "EMP-002-UPDATED",
                null,
                "VT-04",
                DataScope.COMPANY,
                null
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.of(employee));
        when(saveUserPort.save(user))
                .thenReturn(user);
        when(saveEmployeePort.save(employee))
                .thenReturn(employee);
        when(loadOrgUnitPort.findById(new OrgUnitId(15L)))
                .thenReturn(
                        Optional.of(
                                activeOrgUnit(
                                        15L,
                                        "OU-15",
                                        "Phòng hiện tại"
                                )
                        )
                );

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
        when(authorizationService.require(
                PermissionCode.USER_UPDATE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "john_doe",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        Employee employee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                15L,
                "EMP-002",
                "John Doe",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        UpdateUserCommand command = new UpdateUserCommand(
                2L,
                "John Doe Updated",
                "john.updated@company.com",
                "EMP-002-UPDATED",
                25L,
                "VT-04",
                DataScope.COMPANY,
                null
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.of(employee));
        when(saveUserPort.save(user))
                .thenReturn(user);
        when(saveEmployeePort.save(employee))
                .thenReturn(employee);
        when(loadOrgUnitPort.findById(new OrgUnitId(25L)))
                .thenReturn(
                        Optional.of(
                                activeOrgUnit(
                                        25L,
                                        "OU-25",
                                        "Phòng ban mới"
                                )
                        )
                );

        UserResult result = userService.updateUser(command);

        assertEquals(25L, employee.getOrgUnitId(), "orgUnitId của Employee phải được cập nhật sang đơn vị mới");
        assertEquals(25L, result.getOrgUnitId(), "result.orgUnitId phải phản ánh đơn vị mới");
        assertEquals("John Doe Updated", employee.getFullName());
        assertEquals("Phòng ban mới", result.getOrgUnitName());

        verify(saveEmployeePort).save(employee);
    }

    @Test
    @DisplayName("updateUser ném DuplicateEmployeeCodeException khi mã nhân viên bị trùng với nhân viên khác")
    void testUpdateUser_DuplicateEmployeeCode_ThrowsException() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "john_doe",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        Employee employee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                15L,
                "EMP-002",
                "John Doe",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        UpdateUserCommand command = new UpdateUserCommand(
                2L,
                "John Doe Updated",
                "john.updated@company.com",
                "EMP-EXISTS",
                null,
                "VT-04",
                DataScope.COMPANY,
                null
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.of(employee));
        when(saveUserPort.save(user))
                .thenReturn(user);
        when(loadEmployeePort.existsByEmployeeCodeAndIdNot("EMP-EXISTS", new EmployeeId(20L)))
                .thenReturn(true);

        assertThrows(
                DuplicateEmployeeCodeException.class,
                () -> userService.updateUser(command)
        );

        verify(saveEmployeePort, never()).save(any());
    }

    @Test
    @DisplayName("updateUser không kiểm tra trùng nếu employeeCode được giữ nguyên như cũ")
    void testUpdateUser_SameEmployeeCode_DoesNotQueryDuplicate() {
        when(authorizationService.require(
                PermissionCode.USER_UPDATE
        )).thenReturn(ADMIN_ID);

        User user = new User(
                new UserId(2L),
                "john_doe",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(20L)
        );

        Employee employee = new Employee(
                new EmployeeId(20L),
                new UserId(2L),
                15L,
                "EMP-002",
                "John Doe",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        UpdateUserCommand command = new UpdateUserCommand(
                2L,
                "John Doe Updated",
                "john.updated@company.com",
                "EMP-002",
                null,
                "VT-04",
                DataScope.COMPANY,
                null
        );

        when(loadUserPort.findById(new UserId(2L)))
                .thenReturn(Optional.of(user));
        when(loadRolePort.findByCode(RoleCode.VT_04))
                .thenReturn(Optional.of(staffRole));
        when(loadEmployeePort.findByUserId(new UserId(2L)))
                .thenReturn(Optional.of(employee));
        when(saveUserPort.save(user))
                .thenReturn(user);
        when(saveEmployeePort.save(employee))
                .thenReturn(employee);
        when(loadOrgUnitPort.findById(new OrgUnitId(15L)))
                .thenReturn(
                        Optional.of(
                                activeOrgUnit(
                                        15L,
                                        "OU-15",
                                        "Phòng hiện tại"
                                )
                        )
                );

        UserResult result = userService.updateUser(command);

        assertEquals("EMP-002", result.getFullName() != null ? employee.getEmployeeCode() : null);
        verify(loadEmployeePort, never()).existsByEmployeeCodeAndIdNot(any(), any());
        verify(saveEmployeePort).save(employee);
    }
}
