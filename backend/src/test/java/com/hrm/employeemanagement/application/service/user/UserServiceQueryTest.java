package com.hrm.employeemanagement.application.service.user;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

class UserServiceQueryTest extends BaseUserServiceTest {

    @Test
    @DisplayName("Lấy danh sách người dùng với Batch Resolving Employee và OrgUnit tối ưu")
    void testGetUsers_PaginationAndBatchResolving() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User u1 = testUser(1L, staffRole, 10L);
        u1.changeDataScope(DataScope.COMPANY, null);
        User u2 = testUser(2L, staffRole, 20L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(u1));
        when(loadUserPort.findAll(0, 20)).thenReturn(List.of(u1, u2));
        when(loadUserPort.count()).thenReturn(2L);

        Employee e1 = testEmployee(10L, 1L, 5L, "E-1");
        Employee e2 = testEmployee(20L, 2L, 5L, "E-2");

        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(1L), new UserId(2L))))
                .thenReturn(List.of(e1, e2));

        OrgUnit orgUnit5 = activeOrgUnit(5L, "OU-05", "Phòng Nhân Sự");
        when(loadOrgUnitPort.findAllByIdIn(List.of(5L))).thenReturn(List.of(orgUnit5));

        PageResult<UserResult> pageResult = userService.getUsers(0, 20);

        assertEquals(2, pageResult.getContent().size());
        assertEquals(2L, pageResult.getTotalElements());
        assertEquals(1, pageResult.getTotalPages());
        verify(authorizationService, times(1)).require(PermissionCode.USER_READ);
    }

    @Test
    @DisplayName("Lấy danh sách người dùng theo DataScope ORGANIZATION_BRANCH")
    void testGetUsers_OrganizationBranchScope() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 5L);
        User scopedUser = testUser(2L, staffRole, 20L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.findByOrgUnitBranch(5L, 0, 20)).thenReturn(List.of(scopedUser));
        when(loadUserPort.countByOrgUnitBranch(5L)).thenReturn(1L);

        Employee employee = testEmployee(20L, 2L, 8L, "E-2");
        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(2L)))).thenReturn(List.of(employee));
        when(loadOrgUnitPort.findAllByIdIn(List.of(8L))).thenReturn(List.of(activeOrgUnit(8L, "OU-08", "Nhánh Công nghệ")));

        PageResult<UserResult> pageResult = userService.getUsers(0, 20);

        assertEquals(1, pageResult.getContent().size());
        assertEquals(1L, pageResult.getTotalElements());
        verify(loadUserPort, times(1)).findByOrgUnitBranch(5L, 0, 20);
        verify(loadUserPort, times(1)).countByOrgUnitBranch(5L);
    }

    @Test
    @DisplayName("Lấy danh sách người dùng theo DataScope SELF chỉ trả về current user ở trang đầu")
    void testGetUsers_SelfScope() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(DataScope.SELF, null);
        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));

        Employee employee = testEmployee(10L, ADMIN_ID, 5L, "E-1");
        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(ADMIN_ID)))).thenReturn(List.of(employee));
        when(loadOrgUnitPort.findAllByIdIn(List.of(5L))).thenReturn(List.of(activeOrgUnit(5L, "OU-05", "Phòng Nhân Sự")));

        PageResult<UserResult> pageResult = userService.getUsers(0, 20);

        assertEquals(1, pageResult.getContent().size());
        assertEquals(1L, pageResult.getTotalElements());
    }

    @Test
    @DisplayName("getUsers áp dụng DataScope mới ở request kế tiếp khi scope bị shrink từ COMPANY sang SELF")
    void testGetUsers_ScopeShrinkCompanyToSelf_AppliesOnNextRequest() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID, ADMIN_ID);

        User currentUser = testUser(ADMIN_ID, staffRole, 10L);
        currentUser.changeDataScope(DataScope.COMPANY, null);

        User otherUser = testUser(2L, staffRole, 20L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.findAll(0, 20)).thenReturn(List.of(currentUser, otherUser));
        when(loadUserPort.count()).thenReturn(2L);

        Employee currentEmployee = testEmployee(10L, ADMIN_ID, 5L, "E-1");
        Employee otherEmployee = testEmployee(20L, 2L, 6L, "E-2");

        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(ADMIN_ID), new UserId(2L))))
                .thenReturn(List.of(currentEmployee, otherEmployee));
        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(ADMIN_ID))))
                .thenReturn(List.of(currentEmployee));

        when(loadOrgUnitPort.findAllByIdIn(List.of(5L, 6L)))
                .thenReturn(List.of(activeOrgUnit(5L, "OU-05", "Khối A"), activeOrgUnit(6L, "OU-06", "Khối B")));
        when(loadOrgUnitPort.findAllByIdIn(List.of(5L)))
                .thenReturn(List.of(activeOrgUnit(5L, "OU-05", "Khối A")));

        PageResult<UserResult> firstRequest = userService.getUsers(0, 20);
        assertEquals(2, firstRequest.getContent().size());

        currentUser.changeDataScope(DataScope.SELF, null);

        PageResult<UserResult> secondRequest = userService.getUsers(0, 20);
        assertEquals(1, secondRequest.getContent().size());
        assertEquals(ADMIN_ID, secondRequest.getContent().get(0).getId());
    }

    @Test
    @DisplayName("COMPANY đọc user khác thành công kèm resolve orgUnitName")
    void testGetUserById_CompanyScope_ReadsOtherUser() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = testUser(ADMIN_ID, adminRole, 10L);
        currentUser.changeDataScope(DataScope.COMPANY, null);

        User targetUser = testUser(5L, staffRole, 50L);
        Employee employee = testEmployee(50L, 5L, 8L, "EMP-005");

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.findById(new UserId(5L))).thenReturn(Optional.of(targetUser));
        when(loadEmployeePort.findByUserId(new UserId(5L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(new OrgUnitId(8L))).thenReturn(Optional.of(activeOrgUnit(8L, "OU-08", "Ban Giám Đốc")));

        UserResult result = userService.getUserById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Ban Giám Đốc", result.getOrgUnitName());
    }

    @Test
    @DisplayName("SELF đọc chính mình thành công")
    void testGetUserById_SelfScope_ReadsSelf() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = testUser(ADMIN_ID, staffRole, 10L);
        currentUser.changeDataScope(DataScope.SELF, null);

        Employee employee = testEmployee(10L, ADMIN_ID, 8L, "EMP-001");

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadEmployeePort.findByUserId(new UserId(ADMIN_ID))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(new OrgUnitId(8L))).thenReturn(Optional.of(activeOrgUnit(8L, "OU-08", "Phòng Cá nhân")));

        UserResult result = userService.getUserById(ADMIN_ID);

        assertNotNull(result);
        assertEquals(ADMIN_ID, result.getId());
    }

    @Test
    @DisplayName("SELF đọc user khác bị từ chối trước khi load target")
    void testGetUserById_SelfScope_ReadsOtherUser_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = testUser(ADMIN_ID, staffRole, 10L);
        currentUser.changeDataScope(DataScope.SELF, null);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));

        assertThrows(PermissionDeniedException.class, () -> userService.getUserById(5L));
        verify(loadUserPort, never()).findById(new UserId(5L));
    }

    @Test
    @DisplayName("ORGANIZATION_BRANCH đọc user trong nhánh thành công")
    void testGetUserById_OrganizationBranchScope_ReadsUserInsideBranch() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 10L);
        User targetUser = testUser(5L, staffRole, 50L);
        Employee employee = testEmployee(50L, 5L, 12L, "EMP-005");

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.existsInOrgUnitBranch(5L, 10L)).thenReturn(true);
        when(loadUserPort.findById(new UserId(5L))).thenReturn(Optional.of(targetUser));
        when(loadEmployeePort.findByUserId(new UserId(5L))).thenReturn(Optional.of(employee));
        when(loadOrgUnitPort.findById(new OrgUnitId(12L))).thenReturn(Optional.of(activeOrgUnit(12L, "OU-12", "Team Backend")));

        UserResult result = userService.getUserById(5L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Team Backend", result.getOrgUnitName());
    }

    @Test
    @DisplayName("ORGANIZATION_BRANCH đọc user ngoài nhánh bị từ chối trước khi load target")
    void testGetUserById_OrganizationBranchScope_ReadsUserOutsideBranch_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 10L);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.existsInOrgUnitBranch(5L, 10L)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> userService.getUserById(5L));
        verify(loadUserPort, never()).findById(new UserId(5L));
    }

    @Test
    @DisplayName("Lấy thông tin người dùng theo ID thất bại khi không tìm thấy")
    void testGetUserById_NotFound_ThrowsException() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(ADMIN_ID);

        User currentUser = testUser(ADMIN_ID, staffRole, 10L);
        currentUser.changeDataScope(DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(ADMIN_ID))).thenReturn(Optional.of(currentUser));
        when(loadUserPort.findById(new UserId(999L))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    @DisplayName("getUsers với COMPANY scope trả về toàn bộ user")
    void testGetUsers_CompanyScope_ReturnsAllUsers() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(1L);

        User currentUser = testUser(1L, adminRole, 10L);
        currentUser.changeDataScope(DataScope.COMPANY, null);

        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUser));

        User u2 = testUser(2L, staffRole, 20L);
        when(loadUserPort.findAll(0, 20)).thenReturn(List.of(currentUser, u2));
        when(loadUserPort.count()).thenReturn(2L);

        Employee e1 = testEmployee(10L, 1L, 5L, "E-1");
        Employee e2 = testEmployee(20L, 2L, 6L, "E-2");

        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(1L), new UserId(2L))))
                .thenReturn(List.of(e1, e2));
        when(loadOrgUnitPort.findAllByIdIn(List.of(5L, 6L)))
                .thenReturn(List.of(activeOrgUnit(5L, "OU-05", "Khối A"), activeOrgUnit(6L, "OU-06", "Khối B")));

        PageResult<UserResult> result = userService.getUsers(0, 20);

        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
    }

    @Test
    @DisplayName("getUsers với SELF scope chỉ trả về chính user hiện tại")
    void testGetUsers_SelfScope_ReturnsOnlyCurrentUser() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(1L);

        User currentUser = testUser(1L, staffRole, 10L);
        currentUser.changeDataScope(DataScope.SELF, null);

        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUser));

        Employee employee = testEmployee(10L, 1L, 5L, "E-1");

        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(1L)))).thenReturn(List.of(employee));
        when(loadOrgUnitPort.findAllByIdIn(List.of(5L))).thenReturn(List.of(activeOrgUnit(5L, "OU-05", "Phòng Kỹ thuật")));

        PageResult<UserResult> result = userService.getUsers(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("getUsers với ORGANIZATION_BRANCH chỉ trả user thuộc nhánh được cấp")
    void testGetUsers_OrganizationBranchScope_ReturnsBranchUsers() {
        when(authorizationService.require(PermissionCode.USER_READ)).thenReturn(1L);

        User currentUser = currentUserWithScope(DataScope.ORGANIZATION_BRANCH, 5L);

        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUser));

        User branchUser = testUser(2L, staffRole, 20L);

        when(loadUserPort.findByOrgUnitBranch(5L, 0, 20)).thenReturn(List.of(branchUser));
        when(loadUserPort.countByOrgUnitBranch(5L)).thenReturn(1L);

        Employee branchEmployee = testEmployee(20L, 2L, 8L, "E-2");

        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(2L)))).thenReturn(List.of(branchEmployee));
        when(loadOrgUnitPort.findAllByIdIn(List.of(8L))).thenReturn(List.of(activeOrgUnit(8L, "OU-08", "Team Backend")));

        PageResult<UserResult> result = userService.getUsers(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getId());
    }
}
