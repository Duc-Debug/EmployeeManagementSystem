package com.hrm.employeemanagement.application.service.allocation;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.AllocationOverloadWarningException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NCL-06-CN-003: Phát hiện quá tải khi phân bổ (Allocation Overload Tests)")
class AllocationOverloadUseCaseTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;

    @Mock
    private SaveWeeklyProjectAllocationPort saveAllocationPort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;

    @Mock
    private SaveAuditLogInNewTransactionPort saveAuditLogPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private Project projectMock;

    private ResourceAllocationService service;

    private final Long rmUserId = 10L;
    private final Long nonRmUserId = 20L;
    private final Long employeeId = 100L;
    private final Long projectIdA = 1L;
    private final Long projectIdB = 2L;
    private final Integer year = 2026;
    private final Integer weekNumber = 38;
    private final YearWeek yearWeek = YearWeek.of(year, weekNumber);

    private User rmUser;
    private User nonRmUser;
    private Employee activeEmployee;

    @BeforeEach
    void setUp() {
        service = new ResourceAllocationService(
                authorizationService,
                loadEmployeePort,
                loadProjectPort,
                loadWeeklyAvailabilityPort,
                saveAllocationPort,
                loadAllocationPort,
                saveAuditLogPort,
                loadUserPort,
                loadOrgUnitPort
        );

        // VT-03: Resource Manager with ORGANIZATION_BRANCH DataScope
        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        rmUser = new User(new UserId(rmUserId), "rm_user", "hash", rmRole, UserStatus.ACTIVE,
                new EmployeeId(101L), DataScope.ORGANIZATION_BRANCH, 1L, 1L);

        // VT-01: Executive with COMPANY DataScope (not RM)
        Role nonRmRole = new Role(new RoleId(1L), RoleCode.VT_01, "Ban giám đốc");
        nonRmUser = new User(new UserId(nonRmUserId), "exec_user", "hash", nonRmRole, UserStatus.ACTIVE,
                new EmployeeId(102L), DataScope.COMPANY, null, 1L);

        activeEmployee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP100", "Nguyễn Văn Test",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
    }

    private void setupMocksForRM() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        lenient().when(authorizationService.hasPermission(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS)).thenReturn(true);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(activeEmployee));
        when(loadProjectPort.findById(new ProjectId(projectIdB))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        when(loadOrgUnitPort.existsInOrgUnitBranch(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("TC-01: Cảnh báo quá tải khi nhân sự đã phân bổ 35h/40h, phân bổ thêm 10h -> Vượt 5h")
    void testTC01_OverloadDetected_ThrowsWarningException() {
        setupMocksForRM();

        // 40h khả dụng
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Đã phân bổ 35h cho dự án A
        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(35), 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of(existingAllocation));

        // Phân bổ thêm 10h cho dự án B mà chưa ghi lý do
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(10));

        AllocationOverloadWarningException ex = assertThrows(
                AllocationOverloadWarningException.class,
                () -> service.allocateResource(command)
        );

        assertEquals(BigDecimal.valueOf(40), ex.getAvailableHours());
        assertEquals(BigDecimal.valueOf(45), ex.getAllocatedHours());
        assertEquals(new BigDecimal("5.00"), ex.getOverloadHours());
        assertTrue(ex.getMessage().contains("5.00"));

        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-02: Quản lý nguồn lực (RM) xác nhận phân bổ vượt kèm lý do -> Lưu thành công với isOverloaded = true")
    void testTC02_RMConfirmsOverloadWithReason_SavedSuccessfully() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(35), 0L);
        WeeklyProjectAllocation newAllocation = new WeeklyProjectAllocation(
                2L, employeeId, projectIdB, yearWeek, BigDecimal.valueOf(10), true, "Lý do", rmUserId, null, 0L);

        // Lần 1: load để tính toán (35h); Lần 2: sau khi lưu để calculateCapacity (35h + 10h = 45h)
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of(existingAllocation))
                .thenReturn(List.of(existingAllocation, newAllocation));

        when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // RM gửi kèm lý do
        String reason = "Yêu cầu khẩn cấp từ khách hàng giai đoạn bàn giao";
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(10), reason);

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertTrue(result.isOverAllocated());

        // Kiểm tra chỉ thực thể đang phân bổ (Dự án B) được lưu với trạng thái overload, không ghi đè dự án A
        ArgumentCaptor<WeeklyProjectAllocation> allocationCaptor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort, times(1)).save(allocationCaptor.capture());
        WeeklyProjectAllocation savedB = allocationCaptor.getValue();

        assertEquals(projectIdB, savedB.getProjectId());
        assertTrue(savedB.isOverloaded());
        assertEquals(reason, savedB.getOverloadReason());
        assertEquals(rmUserId, savedB.getOverloadApprovedBy());
        assertNotNull(savedB.getOverloadApprovedAt());
        assertEquals(BigDecimal.valueOf(10), savedB.getAllocatedHours());
    }

    @Test
    @DisplayName("TC-03: Nhân sự nghỉ phép đã duyệt 2 ngày (16h), phân bổ 32h -> Cảnh báo vượt 8h trên 24h khả dụng")
    void testTC03_ApprovedLeaveReducesAvailability_ThrowsWarning() {
        setupMocksForRM();

        // 40h chuẩn - 16h nghỉ phép = 24h khả dụng
        WeeklyAvailability availability = new WeeklyAvailability(
                1L, employeeId, yearWeek, 40, 0, BigDecimal.valueOf(16), BigDecimal.valueOf(24));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of());

        // Phân bổ 32h (chưa có lý do)
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(32));

        AllocationOverloadWarningException ex = assertThrows(
                AllocationOverloadWarningException.class,
                () -> service.allocateResource(command)
        );

        assertEquals(BigDecimal.valueOf(24), ex.getAvailableHours());
        assertEquals(BigDecimal.valueOf(32), ex.getAllocatedHours());
        assertEquals(new BigDecimal("8.00"), ex.getOverloadHours());
        assertTrue(ex.getMessage().contains("8.00"));

        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Người dùng không có quyền RESOURCE_ALLOCATION_OVERLOAD_BYPASS cố tình xác nhận vượt tải -> Chặn 403 và ghi log ACCESS_DENIED_OVERLOAD_CONFIRM")
    void testTC04_NonRMConfirmsOverload_ForbiddenAndAccessDeniedLogged() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(nonRmUserId);
        when(authorizationService.hasPermission(PermissionCode.RESOURCE_ALLOCATION_OVERLOAD_BYPASS)).thenReturn(false);
        when(loadUserPort.findById(new UserId(nonRmUserId))).thenReturn(Optional.of(nonRmUser));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(activeEmployee));
        when(loadProjectPort.findById(new ProjectId(projectIdB))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(35), 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of(existingAllocation));

        // Non-RM gửi request kèm lý do vượt tải
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(10), "Tự ý xác nhận");

        assertThrows(PermissionDeniedException.class, () -> service.allocateResource(command));

        // Xác minh log bảo mật ACCESS_DENIED_OVERLOAD_CONFIRM được ghi
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        AuditLog denialLog = auditCaptor.getValue();

        assertEquals("ACCESS_DENIED_OVERLOAD_CONFIRM", denialLog.getAction());
        assertEquals(nonRmUserId, denialLog.getUserId());
        assertTrue(denialLog.getNewValue().contains("attempted_overload_hours=5.00"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-05: Ghi nhật ký kiểm toán nghiệp vụ ALLOCATION_OVERLOAD_BYPASS phản ánh state transition khi RM xác nhận vượt tải")
    void testTC05_ValidRMConfirmation_LogsAllocationOverloadBypass() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(35), 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of(existingAllocation));

        WeeklyProjectAllocation savedMock = new WeeklyProjectAllocation(
                999L, employeeId, projectIdB, yearWeek, BigDecimal.valueOf(10), true, "Lý do hợp lệ", rmUserId, null, 0L);
        when(saveAllocationPort.save(any())).thenReturn(savedMock);

        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(10), "Lý do hợp lệ");

        service.allocateResource(command);

        // Verify ghi 2 log: 1 RESOURCE_ALLOCATED cho dự án B và 1 ALLOCATION_OVERLOAD_BYPASS cho chính dự án B
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(2)).save(auditCaptor.capture());

        List<AuditLog> logs = auditCaptor.getAllValues();
        AuditLog bypassLog = logs.stream()
                .filter(l -> "ALLOCATION_OVERLOAD_BYPASS".equals(l.getAction()) && Long.valueOf(999L).equals(l.getRecordId()))
                .findFirst()
                .orElse(null);

        assertNotNull(bypassLog, "Bắt buộc phải có audit log ALLOCATION_OVERLOAD_BYPASS");
        assertEquals(rmUserId, bypassLog.getUserId());
        assertEquals(999L, bypassLog.getRecordId());
        assertNotNull(bypassLog.getOldValue(), "oldValue phải lưu state transition");
        assertTrue(bypassLog.getOldValue().contains("isOverloaded=false"));
        assertTrue(bypassLog.getNewValue().contains("isOverloaded=true"));
        assertTrue(bypassLog.getNewValue().contains("overloadHours=5.00"));
        assertTrue(bypassLog.getNewValue().contains("overloadReason=Lý do hợp lệ"));
    }

    @Test
    @DisplayName("UPDATE allocation: Sửa phân bổ trên cùng dự án từ 10h lên 15h không bị double-count và không overload")
    void testUpdateAllocation_NoDoubleCount_Success() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Dự án A: 20h, Dự án B (chính dự án đang sửa): 10h
        WeeklyProjectAllocation allocA = new WeeklyProjectAllocation(1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(20), 0L);
        WeeklyProjectAllocation allocB = new WeeklyProjectAllocation(2L, employeeId, projectIdB, yearWeek, BigDecimal.valueOf(10), 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of(allocA, allocB));

        when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Sửa dự án B từ 10h lên 15h -> Tổng = 20h + 15h = 35h <= 40h
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(15));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertFalse(result.isOverAllocated());

        ArgumentCaptor<WeeklyProjectAllocation> captor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort).save(captor.capture());
        WeeklyProjectAllocation updated = captor.getValue();
        assertEquals(2L, updated.getId());
        assertEquals(BigDecimal.valueOf(15), updated.getAllocatedHours());
        assertFalse(updated.isOverloaded());
    }

    @Test
    @DisplayName("UPDATE allocation with overload: Sửa phân bổ trên cùng dự án từ 20h lên 30h gây overload -> Audit log ALLOCATION_OVERLOAD_BYPASS chỉ ghi cho dự án được điều chỉnh")
    void testUpdateAllocation_WithOverloadBypass_CapturesAccurateOldOverloadState() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Dự án A: 15h, Dự án B (đang sửa): 20h (chưa overload)
        WeeklyProjectAllocation allocA = new WeeklyProjectAllocation(1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(15), 0L);
        WeeklyProjectAllocation allocB = new WeeklyProjectAllocation(2L, employeeId, projectIdB, yearWeek, BigDecimal.valueOf(20), 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of(allocA, allocB));
        when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // RM sửa Dự án B từ 20h lên 30h -> Tổng 15h + 30h = 45h > 40h -> Vượt 5h
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(30), "Ưu tiên hoàn thành sprint");

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertTrue(result.isOverAllocated());

        // Chỉ dự án B được lưu
        verify(saveAllocationPort, times(1)).save(any());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(2)).save(auditCaptor.capture());

        List<AuditLog> bypassLogs = auditCaptor.getAllValues().stream()
                .filter(l -> "ALLOCATION_OVERLOAD_BYPASS".equals(l.getAction()))
                .toList();

        assertEquals(1, bypassLogs.size(), "Chỉ dự án B (được điều chỉnh) mới phát sinh audit log ALLOCATION_OVERLOAD_BYPASS");

        AuditLog bypassLogB = bypassLogs.get(0);
        assertEquals(Long.valueOf(2L), bypassLogB.getRecordId());
        assertEquals("isOverloaded=false;projectAllocatedHours=20;totalWeeklyAllocatedHours=35", bypassLogB.getOldValue(),
                "oldValue phải phản ánh giá trị snapshot 20h và tổng 35h trước khi mutation đối tượng");
        assertTrue(bypassLogB.getNewValue().contains("isOverloaded=true"));
        assertTrue(bypassLogB.getNewValue().contains("projectAllocatedHours=30"));
        assertTrue(bypassLogB.getNewValue().contains("totalWeeklyAllocatedHours=45"));
        assertTrue(bypassLogB.getNewValue().contains("overloadHours=5.00"));
    }

    @Test
    @DisplayName("HIGH-01 / MEDIUM-03: Tuần đã quá tải, cập nhật phân bổ cho dự án B -> Bảo toàn metadata của dự án A độc lập")
    void testReAllocateOverloadedWeek_PreservesIndependentApprovalMetadataAcrossAllocations() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Ban đầu: Dự án A (20h) và Dự án B (25h) đã quá tải với Lý do riêng của từng dự án
        LocalDateTime oldApprovedAt = LocalDateTime.now().minusDays(2);
        Long oldApproverId = 101L;
        String oldReasonA = "Sprint 1 gấp của Dự án A";
        String oldReasonB = "Sprint 1 gấp của Dự án B";

        WeeklyProjectAllocation allocA = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(20), true, oldReasonA, oldApproverId, oldApprovedAt, 0L);
        WeeklyProjectAllocation allocB = new WeeklyProjectAllocation(
                2L, employeeId, projectIdB, yearWeek, BigDecimal.valueOf(25), true, oldReasonB, oldApproverId, oldApprovedAt, 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of(allocA, allocB));
        when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // RM (rmUserId) điều chỉnh Dự án B từ 25h lên 30h (tổng tuần thành 50h, quá tải 10h)
        // Lý do mới: "Bổ sung nhân lực fix bug release"
        String newReason = "Bổ sung nhân lực fix bug release";
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(30), newReason);

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertTrue(result.isOverAllocated());

        // Chỉ dự án B được lưu với metadata mới
        ArgumentCaptor<WeeklyProjectAllocation> captor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort, times(1)).save(captor.capture());

        WeeklyProjectAllocation savedB = captor.getValue();
        assertEquals(projectIdB, savedB.getProjectId());
        assertTrue(savedB.isOverloaded());
        assertEquals(newReason, savedB.getOverloadReason());
        assertEquals(rmUserId, savedB.getOverloadApprovedBy());
        assertNotNull(savedB.getOverloadApprovedAt());
        assertTrue(savedB.getOverloadApprovedAt().isAfter(oldApprovedAt));

        // Dự án A giữ nguyên metadata ban đầu của nó, không bị ghi đè
        assertEquals(oldReasonA, allocA.getOverloadReason(), "Dự án A không bị ghi đè lý do");
        assertEquals(oldApproverId, allocA.getOverloadApprovedBy(), "Dự án A giữ nguyên người phê duyệt gốc");
        assertEquals(oldApprovedAt, allocA.getOverloadApprovedAt(), "Dự án A giữ nguyên thời điểm phê duyệt");

        // Xác nhận audit log ghi nhận ALLOCATION_OVERLOAD_BYPASS chỉ cho dự án B
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(2)).save(auditCaptor.capture());
        List<AuditLog> bypassLogs = auditCaptor.getAllValues().stream()
                .filter(l -> "ALLOCATION_OVERLOAD_BYPASS".equals(l.getAction()))
                .toList();
        assertEquals(1, bypassLogs.size());
        assertEquals(Long.valueOf(2L), bypassLogs.get(0).getRecordId());
    }

    @Test
    @DisplayName("Issue 1: Giảm giờ phân bổ xuống dưới khả dụng -> Xóa cờ isOverloaded trên TẤT CẢ các phân bổ trong tuần")
    void testReduceAllocation_ClearsOverloadOnAllAllocationsForEmployeeWeek() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Ban đầu cả 2 dự án đang mang cờ isOverloaded = true (tổng 20h + 25h = 45h > 40h)
        LocalDateTime approvedAt = LocalDateTime.now().minusDays(1);
        WeeklyProjectAllocation allocA = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(20), true, "Lý do cũ", rmUserId, approvedAt, 0L);
        WeeklyProjectAllocation allocB = new WeeklyProjectAllocation(
                2L, employeeId, projectIdB, yearWeek, BigDecimal.valueOf(25), true, "Lý do cũ", rmUserId, approvedAt, 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of(allocA, allocB));
        when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // User giảm Dự án B từ 25h xuống 15h -> Tổng tuần mới: 20h + 15h = 35h <= 40h (Hết quá tải)
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(15));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertFalse(result.isOverAllocated(), "Tuần không còn bị quá tải");

        // Cả 2 dự án (Dự án A và Dự án B) đều được lưu với clearOverload()
        ArgumentCaptor<WeeklyProjectAllocation> captor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort, times(2)).save(captor.capture());

        List<WeeklyProjectAllocation> savedList = captor.getAllValues();
        WeeklyProjectAllocation savedA = savedList.stream().filter(a -> a.getProjectId().equals(projectIdA)).findFirst().orElseThrow();
        WeeklyProjectAllocation savedB = savedList.stream().filter(a -> a.getProjectId().equals(projectIdB)).findFirst().orElseThrow();

        assertFalse(savedA.isOverloaded(), "Dự án A phải được dọn dẹp cờ overload");
        assertNull(savedA.getOverloadReason());
        assertNull(savedA.getOverloadApprovedBy());

        assertFalse(savedB.isOverloaded(), "Dự án B phải được dọn dẹp cờ overload");
        assertNull(savedB.getOverloadReason());
        assertNull(savedB.getOverloadApprovedBy());

        // Kiểm tra Audit log ALLOCATION_OVERLOAD_CLEARED được ghi cho cả 2 dự án
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort, times(3)).save(auditCaptor.capture());
        List<AuditLog> clearedLogs = auditCaptor.getAllValues().stream()
                .filter(l -> "ALLOCATION_OVERLOAD_CLEARED".equals(l.getAction()))
                .toList();
        assertEquals(2, clearedLogs.size(), "Cả 2 dự án đều phải có audit log ALLOCATION_OVERLOAD_CLEARED");
    }

    @Test
    @DisplayName("Boundary: Phân bổ đúng bằng giờ khả dụng (40 == 40) -> Không overload")
    void testBoundary_AllocatedEqualsAvailable_NotOverloaded() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of());
        when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(40));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertFalse(result.isOverAllocated());
    }

    @Test
    @DisplayName("Validation: Lý do quá tải chỉ gồm khoảng trắng -> Vẫn tính là chưa có lý do và cảnh báo")
    void testBlankReason_TreatedAsMissingReason() {
        setupMocksForRM();

        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of());

        // Phân bổ 45h với reason chỉ gồm spaces
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectIdB, year, weekNumber, BigDecimal.valueOf(45), "   ");

        assertThrows(AllocationOverloadWarningException.class, () -> service.allocateResource(command));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Domain Invariant: markOverloaded yêu cầu approvedBy và approvedAt không được null")
    void testDomainInvariant_MarkOverloaded_RequiresNonNullApproverAndTime() {
        WeeklyProjectAllocation allocation = new WeeklyProjectAllocation(
                1L, employeeId, projectIdA, yearWeek, BigDecimal.valueOf(35), 0L);

        // approvedBy null -> ném NullPointerException
        assertThrows(NullPointerException.class, () ->
                allocation.markOverloaded("Reason", null, java.time.LocalDateTime.now()));

        // approvedAt null -> ném NullPointerException
        assertThrows(NullPointerException.class, () ->
                allocation.markOverloaded("Reason", rmUserId, null));

        // reason blank -> ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                allocation.markOverloaded("   ", rmUserId, java.time.LocalDateTime.now()));

        // Hợp lệ
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        allocation.markOverloaded("Hợp lệ", rmUserId, now);
        assertTrue(allocation.isOverloaded());
        assertEquals("Hợp lệ", allocation.getOverloadReason());
        assertEquals(rmUserId, allocation.getOverloadApprovedBy());
        assertEquals(now, allocation.getOverloadApprovedAt());
    }

    @Test
    @DisplayName("Exception Semantics: AllocationOverloadWarningException kế thừa DomainException, độc lập với AllocationCapacityExceededException")
    void testExceptionSemantics_DirectDomainExceptionInheritance() {
        AllocationOverloadWarningException ex = new AllocationOverloadWarningException(
                "Warning", BigDecimal.valueOf(40), BigDecimal.valueOf(45), BigDecimal.valueOf(5));

        assertTrue(ex instanceof com.hrm.employeemanagement.domain.exception.DomainException);
        assertTrue(com.hrm.employeemanagement.domain.exception.DomainException.class.isAssignableFrom(AllocationOverloadWarningException.class));
        assertFalse(com.hrm.employeemanagement.domain.exception.allocation.AllocationCapacityExceededException.class.isAssignableFrom(AllocationOverloadWarningException.class));
    }
}