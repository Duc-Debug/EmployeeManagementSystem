package com.hrm.employeemanagement.application.service.allocation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationPageResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadAllocationNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.notification.NotificationId;
import com.hrm.employeemanagement.domain.notification.NotificationType;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllocationNotificationsService Tests (BR-05, AC-03, TC-03)")
class GetAllocationNotificationsServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadAllocationNotificationPort loadAllocationNotificationPort;

    @Mock
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    private GetAllocationNotificationsService service;

    private User rmUser;
    private User pmUser;
    private User employeeUser;

    @BeforeEach
    void setUp() {
        service = new GetAllocationNotificationsService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadProjectPort,
                loadAllocationNotificationPort,
                deniedAuditLogPort
        );

        rmUser = new User(
                new UserId(10L), "rm_user", "hash",
                new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"),
                UserStatus.ACTIVE, new EmployeeId(100L),
                DataScope.ORGANIZATION_BRANCH, 5L, 1L
        );

        pmUser = new User(
                new UserId(20L), "pm_user", "hash",
                new Role(new RoleId(2L), RoleCode.VT_02, "Quản lý dự án"),
                UserStatus.ACTIVE, new EmployeeId(200L),
                DataScope.SELF, null, 1L
        );

        employeeUser = new User(
                new UserId(30L), "emp_user", "hash",
                new Role(new RoleId(4L), RoleCode.VT_04, "Nhân viên chuyên môn"),
                UserStatus.ACTIVE, new EmployeeId(300L),
                DataScope.SELF, null, 1L
        );
    }

    @Test
    @DisplayName("Happy: VT-03 (RM) xem danh sách thông báo phân bổ trong bộ phận thành công")
    void rmUser_ViewNotifications_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(10L);
        when(loadUserPort.findById(new UserId(10L))).thenReturn(Optional.of(rmUser));

        when(loadProjectPort.findAllProjectIdsByOrgUnitBranch(5L)).thenReturn(List.of(1L));

        Notification n = new Notification(
                NotificationId.of(1L), new UserId(20L), new UserId(10L),
                NotificationType.ALLOCATION_CHANGED, "PROJECT_ALLOCATION", 1L,
                "Thay đổi phân bổ", "Nội dung", false, LocalDateTime.now()
        );
        when(loadAllocationNotificationPort.findAllocationNotifications(org.mockito.ArgumentMatchers.isNull(), eq(List.of(1L)), eq(0), eq(10)))
                .thenReturn(List.of(n));
        when(loadAllocationNotificationPort.countAllocationNotifications(org.mockito.ArgumentMatchers.isNull(), eq(List.of(1L)))).thenReturn(1L);

        when(loadUserPort.findAllByIdIn(anyList())).thenReturn(List.of(rmUser, pmUser));
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());

        AllocationNotificationPageResult result = service.getAllocationNotifications(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals("Thay đổi phân bổ", result.content().get(0).title());
    }

    @Test
    @DisplayName("Happy: VT-02 (PM) xem danh sách thông báo phân bổ của dự án mình quản lý thành công (chỉ nhận thông báo của chính mình)")
    void pmUser_ViewNotifications_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(20L);
        when(loadUserPort.findById(new UserId(20L))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.existsManagedBy(1L, 200L)).thenReturn(true);

        Notification n = new Notification(
                NotificationId.of(2L), new UserId(20L), new UserId(10L),
                NotificationType.ALLOCATION_CHANGED, "PROJECT_ALLOCATION", 1L,
                "Gỡ phân bổ", "Nội dung", false, LocalDateTime.now()
        );
        // PM chỉ truy vấn notification có recipientId = 20L
        when(loadAllocationNotificationPort.findAllocationNotifications(eq(20L), eq(List.of(1L)), eq(0), eq(10)))
                .thenReturn(List.of(n));
        when(loadAllocationNotificationPort.countAllocationNotifications(eq(20L), eq(List.of(1L)))).thenReturn(1L);

        when(loadUserPort.findAllByIdIn(anyList())).thenReturn(List.of(rmUser, pmUser));
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());

        AllocationNotificationPageResult result = service.getAllocationNotifications(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
    }

    @Test
    @DisplayName("Happy: VT-02 (PM) xem tất cả thông báo của các dự án mình quản lý khi projectId = null")
    void pmUser_ViewAllManagedProjects_Success() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(20L);
        when(loadUserPort.findById(new UserId(20L))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.findAllManagedProjectIds(200L)).thenReturn(List.of(1L, 2L));

        Notification n = new Notification(
                NotificationId.of(3L), new UserId(20L), new UserId(10L),
                NotificationType.ALLOCATION_CHANGED, "PROJECT_ALLOCATION", 2L,
                "Chuyển tuần", "Nội dung", false, LocalDateTime.now()
        );
        when(loadAllocationNotificationPort.findAllocationNotifications(eq(20L), eq(List.of(1L, 2L)), eq(0), eq(10)))
                .thenReturn(List.of(n));
        when(loadAllocationNotificationPort.countAllocationNotifications(eq(20L), eq(List.of(1L, 2L)))).thenReturn(1L);

        when(loadUserPort.findAllByIdIn(anyList())).thenReturn(List.of(rmUser, pmUser));
        when(loadEmployeePort.findAllByIdIn(anyList())).thenReturn(Collections.emptyList());

        AllocationNotificationPageResult result = service.getAllocationNotifications(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals("Chuyển tuần", result.content().get(0).title());
    }

    @Test
    @DisplayName("TC-03 / BR-05: VT-02 cố xem thông báo của dự án KHÔNG do mình quản lý -> 403 + ghi log từ chối")
    void pmUser_ViewOtherProject_ThrowsForbiddenAndAudits() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(20L);
        when(loadUserPort.findById(new UserId(20L))).thenReturn(Optional.of(pmUser));
        when(loadProjectPort.existsManagedBy(99L, 200L)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> service.getAllocationNotifications(99L, 0, 10));

        verify(deniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-03 / BR-05: VT-04 (Nhân viên) cố mở màn hình thông báo phân bổ -> 403 + ghi log từ chối")
    void vt04User_AccessDenied_ThrowsForbiddenAndAudits() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(30L);
        when(loadUserPort.findById(new UserId(30L))).thenReturn(Optional.of(employeeUser));

        assertThrows(PermissionDeniedException.class, () -> service.getAllocationNotifications(null, 0, 10));

        verify(deniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-03 / BR-05: VT-01 (Ban giám đốc) cố mở màn hình thông báo phân bổ -> 403 + ghi log từ chối")
    void vt01User_AccessDenied_ThrowsForbiddenAndAudits() {
        User dirUser = new User(
                new UserId(40L), "dir_user", "hash",
                new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc"),
                UserStatus.ACTIVE, new EmployeeId(400L),
                DataScope.COMPANY, null, 1L
        );
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(40L);
        when(loadUserPort.findById(new UserId(40L))).thenReturn(Optional.of(dirUser));

        assertThrows(PermissionDeniedException.class, () -> service.getAllocationNotifications(null, 0, 10));

        verify(deniedAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Security: VT-03 (RM) có DataScope.SELF khi projectId = null không được rơi vào truy vấn toàn công ty -> 403 Forbidden")
    void rmUser_SelfScope_WithoutProjectId_ThrowsForbiddenAndAudits() {
        User rmUserSelf = org.mockito.Mockito.mock(User.class);
        when(rmUserSelf.getRole()).thenReturn(new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực"));
        when(rmUserSelf.getDataScope()).thenReturn(DataScope.SELF);
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(11L);
        when(loadUserPort.findById(new UserId(11L))).thenReturn(Optional.of(rmUserSelf));

        assertThrows(PermissionDeniedException.class, () -> service.getAllocationNotifications(null, 0, 10));

        verify(deniedAuditLogPort).save(argThat(log ->
                "ACCESS_DENIED_ALLOCATION_NOTIFICATIONS".equals(log.getAction())
                        && log.getNewValue() != null
                        && log.getNewValue().contains("denied_reason=INVALID_DATA_SCOPE_SELF")
        ));
    }
}
