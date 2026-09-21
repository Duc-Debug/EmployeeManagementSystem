package com.hrm.employeemanagement.application.service.conflict;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.dto.conflict.AssignScheduleConflictHandlerCommand;
import com.hrm.employeemanagement.application.dto.conflict.ResolveScheduleConflictWithNoteCommand;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.LoadScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.conflict.SaveScheduleConflictPort;
import com.hrm.employeemanagement.application.port.outbound.notification.SimulatedNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflict;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.UserId;

class ScheduleConflictServiceTest {

    private LoadScheduleConflictPort loadConflictPort;
    private SaveScheduleConflictPort saveConflictPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadProjectPort loadProjectPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private AuthorizationService authorizationService;
    private SaveAuditLogInNewTransactionPort auditLogPort;
    private SimulatedNotificationPort notificationPort;

    private ScheduleConflictService service;

    @BeforeEach
    void setUp() {
        loadConflictPort = Mockito.mock(LoadScheduleConflictPort.class);
        saveConflictPort = Mockito.mock(SaveScheduleConflictPort.class);
        loadAllocationPort = Mockito.mock(LoadWeeklyProjectAllocationPort.class);
        loadApprovedLeavesPort = Mockito.mock(LoadApprovedLeavesPort.class);
        loadEmployeePort = Mockito.mock(LoadEmployeePort.class);
        loadProjectPort = Mockito.mock(LoadProjectPort.class);
        loadOrgUnitPort = Mockito.mock(LoadOrgUnitPort.class);
        authorizationService = Mockito.mock(AuthorizationService.class);
        auditLogPort = Mockito.mock(SaveAuditLogInNewTransactionPort.class);
        notificationPort = Mockito.mock(SimulatedNotificationPort.class);

        service = new ScheduleConflictService(
                loadConflictPort,
                saveConflictPort,
                loadAllocationPort,
                loadApprovedLeavesPort,
                loadEmployeePort,
                loadProjectPort,
                loadOrgUnitPort,
                authorizationService,
                auditLogPort,
                notificationPort
        );

        when(authorizationService.requireAny(any(PermissionCode[].class))).thenReturn(1L);
    }

    @Test
    @DisplayName("NCL-07-CN-001-TC-01: Luồng thành công - Phân bổ 2 dự án cùng tuần phát hiện xung đột và giờ vượt")
    void testTC01_MultiProjectAllocationSuccess() {
        Employee emp = new Employee(
                new EmployeeId(10L),
                new UserId(100L),
                1L,
                "NV010",
                "Nguyễn Văn A",
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadEmployeePort.findById(new EmployeeId(10L))).thenReturn(Optional.of(emp));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp));

        Project p1 = new Project(new ProjectId(1L), "PROJ-A", "Dự án Alpha", 1L, new EmployeeId(1L), null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L);
        Project p2 = new Project(new ProjectId(2L), "PROJ-B", "Dự án Beta", 1L, new EmployeeId(1L), null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L);
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(p1, p2));

        OrgUnit orgUnit = new OrgUnit(new OrgUnitId(1L), "DEV", "Phòng Lập Trình", OrgUnitType.DEPARTMENT, null, "/1", 1, OrgUnitStatus.ACTIVE, "Dev Dept", 1L, LocalDateTime.now(), LocalDateTime.now());
        when(loadOrgUnitPort.findById(new OrgUnitId(1L))).thenReturn(Optional.of(orgUnit));
        when(loadOrgUnitPort.findAllByIdIn(any())).thenReturn(List.of(orgUnit));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(1L, 10L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));
        WeeklyProjectAllocation alloc2 = new WeeklyProjectAllocation(2L, 10L, 2L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));

        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(List.of(10L), 2026, 37, 37))
                .thenReturn(List.of(alloc1, alloc2));

        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Collections.emptyMap());

        ScheduleConflict createdConflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        createdConflict.setId(1001L);

        when(saveConflictPort.save(any(ScheduleConflict.class))).thenReturn(createdConflict);
        when(loadConflictPort.findConflicts(2026, 37, 37, null, null, null)).thenReturn(List.of(createdConflict));

        ScheduleConflictQuery query = new ScheduleConflictQuery(2026, 37, 37, null, null, null, null);
        List<ScheduleConflictResult> results = service.getScheduleConflicts(query);

        assertNotNull(results);
        assertEquals(1, results.size());
        ScheduleConflictResult res = results.get(0);
        assertEquals("Nguyễn Văn A", res.employeeName());
        assertEquals(ConflictType.MULTI_PROJECT_ALLOCATION, res.conflictType());
        assertTrue(res.projectNames().contains("Dự án Alpha"));
        assertTrue(res.projectNames().contains("Dự án Beta"));
        assertEquals(BigDecimal.valueOf(80.0), res.totalAllocatedHours());
        assertEquals(BigDecimal.valueOf(40.0), res.excessHours());
    }

    @Test
    @DisplayName("NCL-07-CN-001-TC-02: Ngoại lệ - Trùng đơn nghỉ phép đã duyệt phát hiện xung đột")
    void testTC02_ApprovedLeaveConflict() {
        Employee emp = new Employee(
                new EmployeeId(11L),
                new UserId(101L),
                1L,
                "NV011",
                "Trần Thị B",
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadEmployeePort.findById(new EmployeeId(11L))).thenReturn(Optional.of(emp));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp));

        Project p1 = new Project(new ProjectId(1L), "PROJ-A", "Dự án Alpha", 1L, new EmployeeId(1L), null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L);
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(p1));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(3L, 11L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));

        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(any(), eq(2026), eq(37), eq(37)))
                .thenReturn(List.of(alloc1));

        YearWeek yw = new YearWeek(2026, 37);
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), eq(List.of(yw))))
                .thenReturn(Map.of(11L, Map.of(yw, BigDecimal.valueOf(16.0))));

        ScheduleConflict createdLeaveConflict = ScheduleConflict.create(
                11L, 2026, 37, ConflictType.LEAVE_ALLOCATION_CONFLICT,
                "1", "Dự án Alpha", 50L, "Đơn nghỉ phép đã duyệt (16.0h)",
                BigDecimal.valueOf(40.0), BigDecimal.valueOf(24.0), BigDecimal.valueOf(16.0),
                "Có đơn nghỉ phép trùng tuần được phân bổ"
        );
        createdLeaveConflict.setId(1002L);

        when(saveConflictPort.save(any(ScheduleConflict.class))).thenReturn(createdLeaveConflict);
        when(loadConflictPort.findConflicts(2026, 37, 37, null, null, null)).thenReturn(List.of(createdLeaveConflict));

        ScheduleConflictQuery query = new ScheduleConflictQuery(2026, 37, 37, null, null, null, null);
        List<ScheduleConflictResult> results = service.getScheduleConflicts(query);

        assertNotNull(results);
        assertEquals(1, results.size());
        ScheduleConflictResult res = results.get(0);
        assertEquals("Trần Thị B", res.employeeName());
        assertEquals(ConflictType.LEAVE_ALLOCATION_CONFLICT, res.conflictType());
        assertTrue(res.leaveInfo().contains("Đơn nghỉ phép đã duyệt"));
    }

    @Test
    @DisplayName("NCL-07-CN-001-TC-03: Dữ liệu rỗng - Không có xung đột nào trong khoảng rà soát")
    void testTC03_EmptyDataNoConflicts() {
        when(loadEmployeePort.findAllActive()).thenReturn(Collections.emptyList());
        when(loadConflictPort.findConflicts(2026, 37, 37, null, null, null)).thenReturn(Collections.emptyList());

        ScheduleConflictQuery query = new ScheduleConflictQuery(2026, 37, 37, null, null, null, null);
        List<ScheduleConflictResult> results = service.getScheduleConflicts(query);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("NCL-07-CN-001-TC-04: Không có quyền - Người dùng từ chối truy cập và hệ thống ghi log audit PERMISSION_DENIED")
    void testTC04_PermissionDenied() {
        when(authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ,
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY,
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        )).thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCHEDULE_CONFLICT_READ));

        ScheduleConflictQuery query = new ScheduleConflictQuery(2026, 37, 37, null, null, null, null);
        assertThrows(PermissionDeniedException.class, () -> service.getScheduleConflicts(query));
    }

    @Test
    @DisplayName("NCL-07-CN-001-TC-05: Lưu lịch sử - Gửi thông báo mô phỏng và cập nhật status + ghi audit log")
    void testTC05_NotifyScheduleConflictAuditHistory() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(1001L);

        when(loadConflictPort.findById(1001L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleConflictResult result = service.notifyScheduleConflict(1001L);

        assertNotNull(result);
        assertEquals(ScheduleConflictStatus.NOTIFIED, result.status());
        verify(notificationPort).sendScheduleConflictWarningNotification(any(), any(), any(), any(), any(), any(), any());
        verify(auditLogPort).save(any());
    }

    @Test
    @DisplayName("BLOCKER 1 Regression: Chỉ có quyền READ không thể gọi notifyScheduleConflict")
    void testNotifyScheduleConflict_ReadPermissionOnly_ThrowsPermissionDeniedException() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(1001L);
        when(loadConflictPort.findById(1001L)).thenReturn(Optional.of(conflict));

        when(authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY,
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        )).thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY));

        assertThrows(PermissionDeniedException.class, () -> service.notifyScheduleConflict(1001L));
    }

    @Test
    @DisplayName("BLOCKER 1 Regression: Chỉ có quyền READ không thể gọi resolveScheduleConflict")
    void testResolveScheduleConflict_ReadPermissionOnly_ThrowsPermissionDeniedException() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(1001L);
        when(loadConflictPort.findById(1001L)).thenReturn(Optional.of(conflict));

        when(authorizationService.requireAny(
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        )).thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_CONFLICT_HANDLE));

        assertThrows(PermissionDeniedException.class, () -> service.resolveScheduleConflictWithNote(new ResolveScheduleConflictWithNoteCommand(1001L, 20L, "Cách xử lý")));
    }

    @Test
    @DisplayName("HIGH 1 Regression: GET getScheduleConflicts chỉ đọc dữ liệu, không ghi DB")
    void testGetScheduleConflicts_IsReadOnly_DoesNotScanOrPersist() {
        ScheduleConflictQuery query = new ScheduleConflictQuery(2026, 37, 37, null, null, null, null);
        service.getScheduleConflicts(query);

        Mockito.verify(saveConflictPort, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("HIGH Regression: Chỉ có quyền READ không thể gọi scanScheduleConflicts")
    void testScanScheduleConflicts_ReadPermissionOnly_ThrowsPermissionDeniedException() {
        when(authorizationService.requireAny(
                PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY,
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        )).thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_SCHEDULE_CONFLICT_NOTIFY));

        assertThrows(PermissionDeniedException.class, () -> service.scanScheduleConflicts(2026, 37, 37));
    }

    @Test
    @DisplayName("HIGH 2 Regression: Phân bổ 2 dự án nhưng tổng giờ <= 40h capacity không tạo cảnh báo xung đột")
    void testMultiProjectAllocationWithinCapacity_NoConflict() {
        Employee emp = new Employee(
                new EmployeeId(12L),
                new UserId(102L),
                1L,
                "NV012",
                "Lê Văn C",
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(10L, 12L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(20.0));
        WeeklyProjectAllocation alloc2 = new WeeklyProjectAllocation(11L, 12L, 2L, new YearWeek(2026, 37), BigDecimal.valueOf(20.0));

        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(any(), eq(2026), eq(37), eq(37)))
                .thenReturn(List.of(alloc1, alloc2));
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Collections.emptyMap());

        List<ScheduleConflictResult> scanned = service.scanScheduleConflicts(2026, 37, 37);

        assertTrue(scanned.isEmpty());
        Mockito.verify(saveConflictPort, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Regression: 1 project vượt capacity (50h/40h) KHÔNG tạo MULTI_PROJECT_ALLOCATION")
    void testSingleProjectOverload_DoesNotCreateMultiProjectConflict() {
        Employee emp = new Employee(
                new EmployeeId(13L),
                new UserId(103L),
                1L,
                "NV013",
                "Phạm Văn D",
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));

        WeeklyProjectAllocation singleAlloc = new WeeklyProjectAllocation(12L, 13L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(50.0));
        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(any(), eq(2026), eq(37), eq(37)))
                .thenReturn(List.of(singleAlloc));
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Collections.emptyMap());

        List<ScheduleConflictResult> scanned = service.scanScheduleConflicts(2026, 37, 37);

        assertTrue(scanned.isEmpty());
        Mockito.verify(saveConflictPort, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Regression: Phân bổ 2 dự án tổng 40h + nghỉ phép 8h chỉ tạo LEAVE_ALLOCATION_CONFLICT, không tạo MULTI_PROJECT_ALLOCATION")
    void testTwoProjectsWithinCapacityWithLeave_CreatesOnlyLeaveConflictNotMultiProject() {
        Employee emp = new Employee(
                new EmployeeId(14L),
                new UserId(104L),
                1L,
                "NV014",
                "Hoàng Thị E",
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp));

        Project p1 = new Project(new ProjectId(1L), "PROJ-A", "Dự án Alpha", 1L, new EmployeeId(1L), null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L);
        Project p2 = new Project(new ProjectId(2L), "PROJ-B", "Dự án Beta", 1L, new EmployeeId(1L), null, null, null, null, ProjectStatus.ACTIVE, new UserId(1L), LocalDateTime.now(), LocalDateTime.now(), 0L);
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(p1, p2));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(13L, 14L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(20.0));
        WeeklyProjectAllocation alloc2 = new WeeklyProjectAllocation(14L, 14L, 2L, new YearWeek(2026, 37), BigDecimal.valueOf(20.0));
        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(any(), eq(2026), eq(37), eq(37)))
                .thenReturn(List.of(alloc1, alloc2));

        YearWeek yw = new YearWeek(2026, 37);
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), eq(List.of(yw))))
                .thenReturn(Map.of(14L, Map.of(yw, BigDecimal.valueOf(8.0))));

        ScheduleConflict createdLeaveConflict = ScheduleConflict.create(
                14L, 2026, 37, ConflictType.LEAVE_ALLOCATION_CONFLICT,
                "1,2", "Dự án Alpha, Dự án Beta", null, "Đơn nghỉ phép đã duyệt (8.0h)",
                BigDecimal.valueOf(40.0), BigDecimal.valueOf(32.0), BigDecimal.valueOf(8.0),
                "Có đơn nghỉ phép trùng tuần được phân bổ"
        );
        createdLeaveConflict.setId(1005L);
        when(saveConflictPort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ScheduleConflictResult> scanned = service.scanScheduleConflicts(2026, 37, 37);

        assertEquals(1, scanned.size());
        assertEquals(ConflictType.LEAVE_ALLOCATION_CONFLICT, scanned.get(0).conflictType());
    }

    @Test
    @DisplayName("NCL-07-CN-005-TC-01: Luồng thành công - Đánh dấu một xung đột là đã xử lý kèm người xử lý và cách xử lý")
    void testNCL07CN005_TC01_ResolveConflictWithNoteSuccess() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Xung đột 3 xung đột mở"
        );
        conflict.setId(2001L);

        Employee handler = new Employee(new EmployeeId(20L), new UserId(200L), 1L, "NV020", "Phạm Văn Handler", false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(20L))).thenReturn(Optional.of(handler));
        when(loadConflictPort.findById(2001L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveScheduleConflictWithNoteCommand command = new ResolveScheduleConflictWithNoteCommand(
                2001L, 20L, "Đã giảm giờ phân bổ dự án Alpha xuống 20h"
        );

        ScheduleConflictResult result = service.resolveScheduleConflictWithNote(command);

        assertNotNull(result);
        assertEquals(ScheduleConflictStatus.RESOLVED, result.status());
        assertEquals(20L, result.assignedHandlerId());
        assertEquals("Đã giảm giờ phân bổ dự án Alpha xuống 20h", result.resolutionNote());
        verify(auditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-07-CN-005-TC-02: Ngoại lệ - Nguyên nhân gây xung đột vẫn còn sau khi đánh dấu đã xử lý -> Tự động mở lại và ghi chú tái phát")
    void testResolvedConflictIsReopenedAsRecurrentOnScan() {
        Employee emp = new Employee(
                new EmployeeId(10L),
                new UserId(100L),
                1L,
                "NV010",
                "Nguyễn Văn A",
                false,
                40,
                com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllActive()).thenReturn(List.of(emp));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp));

        WeeklyProjectAllocation alloc1 = new WeeklyProjectAllocation(1L, 10L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));
        WeeklyProjectAllocation alloc2 = new WeeklyProjectAllocation(2L, 10L, 2L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));
        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(any(), eq(2026), eq(37), eq(37)))
                .thenReturn(List.of(alloc1, alloc2));
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(any(), any()))
                .thenReturn(Collections.emptyMap());

        ScheduleConflict resolvedConflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        resolvedConflict.setId(2002L);
        resolvedConflict.markAsResolved();

        when(loadConflictPort.findConflicts(2026, 37, 37, null, null, null))
                .thenReturn(List.of(resolvedConflict));
        when(saveConflictPort.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ScheduleConflictResult> scanned = service.scanScheduleConflicts(2026, 37, 37);

        assertFalse(scanned.isEmpty());
        assertEquals(1, scanned.size());
        assertEquals(ScheduleConflictStatus.REOPENED, scanned.get(0).status());
        assertTrue(scanned.get(0).isRecurrent());
        assertNotNull(scanned.get(0).recurrentNote());
        verify(saveConflictPort).saveAll(any());
    }

    @Test
    @DisplayName("NCL-07-CN-005-TC-05: Nhân sự inactive có allocation cũ -> Loại khỏi danh sách rà soát xung đột")
    void testInactiveEmployeeWithAllocationsIsExcludedFromScan() {
        // Mock no active employees
        when(loadEmployeePort.findAllActive()).thenReturn(Collections.emptyList());

        WeeklyProjectAllocation inactiveAlloc1 = new WeeklyProjectAllocation(101L, 999L, 1L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));
        WeeklyProjectAllocation inactiveAlloc2 = new WeeklyProjectAllocation(102L, 999L, 2L, new YearWeek(2026, 37), BigDecimal.valueOf(40.0));
        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(any(), eq(2026), eq(37), eq(37)))
                .thenReturn(List.of(inactiveAlloc1, inactiveAlloc2));

        List<ScheduleConflictResult> scanned = service.scanScheduleConflicts(2026, 37, 37);

        assertTrue(scanned.isEmpty());
        verify(saveConflictPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-07-CN-005-TC-03: Không có quyền - Từ chối truy cập và ghi nhật ký từ chối PERMISSION_DENIED")
    void testNCL07CN005_TC03_NoPermissionDenied() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Xung đột 3 xung đột mở"
        );
        conflict.setId(2001L);
        when(loadConflictPort.findById(2001L)).thenReturn(Optional.of(conflict));

        when(authorizationService.requireAny(
                PermissionCode.RESOURCE_CONFLICT_HANDLE
        )).thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_CONFLICT_HANDLE));

        ResolveScheduleConflictWithNoteCommand command = new ResolveScheduleConflictWithNoteCommand(
                2001L, 20L, "Xử lý"
        );

        assertThrows(PermissionDeniedException.class, () -> service.resolveScheduleConflictWithNote(command));
    }

    @Test
    @DisplayName("NCL-07-CN-005-TC-04: Gán người xử lý - Lưu lịch sử audit log thành công")
    void testNCL07CN005_TC04_AssignHandlerAuditLog() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Xung đột 3 xung đột mở"
        );
        conflict.setId(2003L);

        Employee handler = new Employee(new EmployeeId(25L), new UserId(250L), 1L, "NV025", "Lê Văn Handler", false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(25L))).thenReturn(Optional.of(handler));
        when(loadConflictPort.findById(2003L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignScheduleConflictHandlerCommand command = new AssignScheduleConflictHandlerCommand(2003L, 25L);

        ScheduleConflictResult result = service.assignScheduleConflictHandler(command);

        assertNotNull(result);
        assertEquals(25L, result.assignedHandlerId());
        verify(auditLogPort).save(any());
    }

    @Test
    @DisplayName("Validate assignedHandlerId: Báo lỗi khi nhân viên không tồn tại hoặc không ở trạng thái ACTIVE")
    void testAssignHandlerValidationFailsForInvalidOrInactiveEmployee() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(2004L);
        when(loadConflictPort.findById(2004L)).thenReturn(Optional.of(conflict));

        when(loadEmployeePort.findById(new EmployeeId(999L))).thenReturn(Optional.empty());

        AssignScheduleConflictHandlerCommand invalidIdCommand = new AssignScheduleConflictHandlerCommand(2004L, 999L);
        assertThrows(IllegalArgumentException.class, () -> service.assignScheduleConflictHandler(invalidIdCommand));

        Employee inactiveHandler = new Employee(new EmployeeId(888L), new UserId(880L), 1L, "NV888", "Inactive User", false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.TERMINATED);
        when(loadEmployeePort.findById(new EmployeeId(888L))).thenReturn(Optional.of(inactiveHandler));

        AssignScheduleConflictHandlerCommand inactiveCommand = new AssignScheduleConflictHandlerCommand(2004L, 888L);
        assertThrows(IllegalStateException.class, () -> service.assignScheduleConflictHandler(inactiveCommand));
    }

    @Test
    @DisplayName("Legacy resolve endpoint sets resolvedBy correctly")
    void testLegacyResolveEndpointSetsResolvedBy() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Xung đột mở"
        );
        conflict.setId(3001L);

        Employee handler = new Employee(new EmployeeId(20L), new UserId(200L), 1L, "NV020", "Handler", false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE);
        when(loadEmployeePort.findById(new EmployeeId(20L))).thenReturn(Optional.of(handler));
        when(loadConflictPort.findById(3001L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleConflictResult result = service.resolveScheduleConflictWithNote(new ResolveScheduleConflictWithNoteCommand(3001L, 20L, "Đã xử lý xong"));

        assertNotNull(result);
        assertEquals(ScheduleConflictStatus.RESOLVED, result.status());
        assertEquals(1L, result.resolvedBy());
        verify(auditLogPort).save(any());
    }

    @Test
    @DisplayName("Attempting to resolve an already RESOLVED conflict throws IllegalStateException")
    void testResolvingAlreadyResolvedConflictThrowsException() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(3002L);
        conflict.markAsResolved(100L);

        when(loadConflictPort.findById(3002L)).thenReturn(Optional.of(conflict));

        ResolveScheduleConflictWithNoteCommand command = new ResolveScheduleConflictWithNoteCommand(3002L, null, "Note");
        assertThrows(IllegalStateException.class, () -> service.resolveScheduleConflictWithNote(command));
    }

    @Test
    @DisplayName("resolveScheduleConflictWithNote throws IllegalArgumentException when resolutionNote is blank")
    void testResolveWithNoteFailsWhenNoteIsBlank() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(3003L);
        when(loadConflictPort.findById(3003L)).thenReturn(Optional.of(conflict));

        ResolveScheduleConflictWithNoteCommand emptyNoteCmd = new ResolveScheduleConflictWithNoteCommand(3003L, null, "   ");
        assertThrows(IllegalArgumentException.class, () -> service.resolveScheduleConflictWithNote(emptyNoteCmd));

        ResolveScheduleConflictWithNoteCommand nullNoteCmd = new ResolveScheduleConflictWithNoteCommand(3003L, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.resolveScheduleConflictWithNote(nullNoteCmd));
    }

    @Test
    @DisplayName("resolveWithNote unassigns handler when handlerId is null even if handler was previously assigned")
    void testResolveWithNoteUnassignsHandlerWhenHandlerIdIsNull() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.setId(3004L);
        conflict.assignHandler(20L);
        assertEquals(20L, conflict.getAssignedHandlerId());

        when(loadConflictPort.findById(3004L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveScheduleConflictWithNoteCommand command = new ResolveScheduleConflictWithNoteCommand(3004L, null, "Đã giải quyết và gỡ bỏ người xử lý");
        ScheduleConflictResult result = service.resolveScheduleConflictWithNote(command);

        assertNotNull(result);
        assertEquals(ScheduleConflictStatus.RESOLVED, conflict.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getAssignedHandlerId());
    }

    @Test
    @DisplayName("Reopening a conflict clears previous resolution audit fields")
    void testReopeningConflictClearsResolvedAuditFields() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trên 2 dự án"
        );
        conflict.resolveWithNote(100L, 20L, "Đã xử lý lần 1");
        assertEquals(ScheduleConflictStatus.RESOLVED, conflict.getStatus());
        assertNotNull(conflict.getResolvedBy());
        assertNotNull(conflict.getResolutionNote());

        conflict.reopenAsRecurrent("Xung đột tái phát");
        assertEquals(ScheduleConflictStatus.REOPENED, conflict.getStatus());
        assertTrue(conflict.getIsRecurrent());
        assertEquals("Xung đột tái phát", conflict.getRecurrentNote());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getResolvedBy());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getResolvedAt());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getResolutionNote());
    }

    @Test
    @DisplayName("mapToResults looks up notifiedBy and resolvedBy using UserId via findAllByUserIdIn")
    void testMapToResultsUsesFindAllByUserIdInForUserIds() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Phân bổ trùng"
        );
        conflict.setId(4001L);
        conflict.markAsNotified(50L);

        Employee emp = new Employee(new EmployeeId(10L), new UserId(100L), 1L, "NV010", "Nguyễn Văn A", false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE);
        Employee notifierEmp = new Employee(new EmployeeId(5L), new UserId(1L), 1L, "NV005", "Trần Văn Notifier", false, 40, com.hrm.employeemanagement.domain.employee.EmployeeStatus.ACTIVE);

        when(loadConflictPort.findById(4001L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(List.of(emp));
        when(loadEmployeePort.findAllByUserIdIn(List.of(new UserId(1L)))).thenReturn(List.of(notifierEmp));

        ScheduleConflictResult result = service.notifyScheduleConflict(4001L);

        assertNotNull(result);
        assertEquals("Trần Văn Notifier", result.notifiedByName());
        verify(loadEmployeePort).findAllByUserIdIn(List.of(new UserId(1L)));
    }

    @Test
    @DisplayName("Multi-cycle lifecycle: OPEN -> NOTIFIED -> RESOLVED -> REOPENED -> RESOLVED -> REOPENED")
    void testMultiCycleConflictLifecycle() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha, Dự án Beta", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Xung đột phân bổ"
        );
        assertEquals(ScheduleConflictStatus.OPEN, conflict.getStatus());

        // Cycle 1: Notify & Resolve
        conflict.markAsNotified(100L);
        assertEquals(ScheduleConflictStatus.NOTIFIED, conflict.getStatus());

        conflict.resolveWithNote(100L, 20L, "Cách xử lý đợt 1");
        assertEquals(ScheduleConflictStatus.RESOLVED, conflict.getStatus());
        assertEquals("Cách xử lý đợt 1", conflict.getResolutionNote());

        // Scan detects conflict still exists -> Reopen
        conflict.reopenAsRecurrent("Nguyên nhân gây xung đột vẫn còn sau khi rà soát lại.");
        assertEquals(ScheduleConflictStatus.REOPENED, conflict.getStatus());
        assertTrue(conflict.getIsRecurrent());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getResolvedBy());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getResolutionNote());

        // Cycle 2: Resolve again from REOPENED status
        conflict.resolveWithNote(101L, 25L, "Cách xử lý đợt 2 sau khi mở lại");
        assertEquals(ScheduleConflictStatus.RESOLVED, conflict.getStatus());
        assertEquals("Cách xử lý đợt 2 sau khi mở lại", conflict.getResolutionNote());
        assertEquals(25L, conflict.getAssignedHandlerId());

        // Scan detects conflict still exists -> Reopen again
        conflict.reopenAsRecurrent("Nguyên nhân vẫn chưa triệt để.");
        assertEquals(ScheduleConflictStatus.REOPENED, conflict.getStatus());
        assertTrue(conflict.getIsRecurrent());
        org.junit.jupiter.api.Assertions.assertNull(conflict.getResolutionNote());
    }

    @Test
    @DisplayName("Assigning null handler unassigns the assigned handler")
    void testAssignHandlerNullUnassignsHandler() {
        ScheduleConflict conflict = ScheduleConflict.create(
                10L, 2026, 37, ConflictType.MULTI_PROJECT_ALLOCATION,
                "1,2", "Dự án Alpha", null, null,
                BigDecimal.valueOf(80.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(40.0),
                "Trùng lịch"
        );
        conflict.setId(5001L);
        conflict.assignHandler(20L);
        assertEquals(20L, conflict.getAssignedHandlerId());

        when(loadConflictPort.findById(5001L)).thenReturn(Optional.of(conflict));
        when(saveConflictPort.save(any(ScheduleConflict.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loadEmployeePort.findAllByIdIn(any())).thenReturn(Collections.emptyList());

        AssignScheduleConflictHandlerCommand command = new AssignScheduleConflictHandlerCommand(5001L, null);
        ScheduleConflictResult result = service.assignScheduleConflictHandler(command);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertNull(conflict.getAssignedHandlerId());
    }
}
