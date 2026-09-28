package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.ApproveUnavailabilityCommand;
import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.availability.SaveWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.SaveUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
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
import com.hrm.employeemanagement.domain.exception.unavailability.UnavailabilityConflictException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApproveUnavailabilityDeclarationServiceTest {

    private LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private SaveUnavailabilityDeclarationPort saveUnavailabilityPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private AuthorizationService authorizationService;
    private SaveAuditLogPort saveAuditLogPort;
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private SaveWeeklyAvailabilityPort saveWeeklyAvailabilityPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private SaveWeeklyProjectAllocationPort saveAllocationPort;
    private LoadHolidaysPort loadHolidaysPort;
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    private ApproveUnavailabilityDeclarationService service;

    @BeforeEach
    void setUp() {
        loadUnavailabilityPort = mock(LoadUnavailabilityDeclarationPort.class);
        saveUnavailabilityPort = mock(SaveUnavailabilityDeclarationPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        authorizationService = mock(AuthorizationService.class);
        saveAuditLogPort = mock(SaveAuditLogPort.class);
        loadWeeklyAvailabilityPort = mock(LoadWeeklyAvailabilityPort.class);
        saveWeeklyAvailabilityPort = mock(SaveWeeklyAvailabilityPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        saveAllocationPort = mock(SaveWeeklyProjectAllocationPort.class);
        loadHolidaysPort = mock(LoadHolidaysPort.class);
        loadApprovedLeavesPort = mock(LoadApprovedLeavesPort.class);

        service = new ApproveUnavailabilityDeclarationService(
                loadUnavailabilityPort,
                saveUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService,
                saveAuditLogPort,
                loadWeeklyAvailabilityPort,
                saveWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                null
        );
    }

    @Test
    @DisplayName("NCL-13-CN-003-TC-01: Người dùng đi đào tạo hai ngày tuần sau -> Khai báo và được quản lý duyệt -> Giờ khả dụng của tuần đó giảm tương ứng hai ngày (16h)")
    void testApproveReducesWeeklyCapacityByTwoDays() {
        Long managerUserId = 99L;
        Long employeeId = 5L;
        Long declarationId = 100L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(managerUserId);

        Role managerRole = mock(Role.class);
        when(managerRole.getCode()).thenReturn(RoleCode.VT_03);
        User managerUser = mock(User.class);
        when(managerUser.getIdValue()).thenReturn(managerUserId);
        when(managerUser.getRole()).thenReturn(managerRole);
        when(managerUser.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(managerUserId))).thenReturn(Optional.of(managerUser));

        // Khai báo 2 ngày đào tạo (Thứ 3 và Thứ 4: 2026-09-22 đến 2026-09-23)
        LocalDate startDate = LocalDate.of(2026, 9, 22);
        LocalDate endDate = LocalDate.of(2026, 9, 23);
        UnavailabilityDeclaration declaration = new UnavailabilityDeclaration(
                declarationId,
                employeeId,
                startDate,
                endDate,
                UnavailabilityReasonType.TRAINING,
                "Đào tạo 2 ngày",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0L
        );
        when(loadUnavailabilityPort.findByIdForUpdate(declarationId)).thenReturn(Optional.of(declaration));
        when(saveUnavailabilityPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee employee = new Employee(
                new EmployeeId(employeeId),
                new UserId(10L),
                1L,
                "EMP005",
                "Trần Văn E",
                "DEV",
                LocalDate.of(2022, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        // Tuần 39 năm 2026 (chứa ngày 2026-09-22)
        YearWeek yw = YearWeek.from(startDate);

        // Giả sử tuần này ban đầu có WeeklyAvailability chuẩn 40h, chưa nghỉ phép, chưa ngày lễ
        WeeklyAvailability initialAvail = WeeklyAvailability.createCalculated(
                employeeId, yw, 40, 0, BigDecimal.ZERO);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yw)).thenReturn(Optional.of(initialAvail));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of());
        when(loadUnavailabilityPort.findApprovedByEmployeeIdAndDateRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(declaration));

        ApproveUnavailabilityCommand command = new ApproveUnavailabilityCommand(declarationId, "Đã duyệt", false);
        UnavailabilityDeclarationResult result = service.approve(command);

        assertNotNull(result);
        assertEquals(UnavailabilityStatus.APPROVED, result.status());

        // Kiểm tra TC-01: Giờ khả dụng của tuần giảm tương ứng 16h (từ 40h còn 24h)
        ArgumentCaptor<WeeklyAvailability> availCaptor = ArgumentCaptor.forClass(WeeklyAvailability.class);
        verify(saveWeeklyAvailabilityPort).save(availCaptor.capture());
        WeeklyAvailability savedAvail = availCaptor.getValue();
        assertEquals(BigDecimal.valueOf(24.00).setScale(2), savedAvail.getNetAvailableHours());

        // Kiểm tra TC-04: Lưu nhật ký kiểm toán APPROVE_UNAVAILABILITY
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(saveAuditLogPort).save(auditCaptor.capture());
        assertEquals("APPROVE_UNAVAILABILITY", auditCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("NCL-13-CN-003-TC-02 & QTN-24: Khoảng thời gian khai báo trùng với phân bổ đã có -> Cảnh báo xung đột trước khi duyệt; khi duyệt, giữ nguyên số giờ phân bổ theo QTN-24")
    void testApproveWithExistingAllocationWarnsConflictAndPreservesAllocations() {
        Long managerUserId = 99L;
        Long employeeId = 5L;
        Long declarationId = 101L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(managerUserId);

        Role managerRole = mock(Role.class);
        when(managerRole.getCode()).thenReturn(RoleCode.VT_03);
        User managerUser = mock(User.class);
        when(managerUser.getIdValue()).thenReturn(managerUserId);
        when(managerUser.getRole()).thenReturn(managerRole);
        when(managerUser.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(managerUserId))).thenReturn(Optional.of(managerUser));

        LocalDate startDate = LocalDate.of(2026, 9, 22);
        LocalDate endDate = LocalDate.of(2026, 9, 23);
        UnavailabilityDeclaration declaration = new UnavailabilityDeclaration(
                declarationId,
                employeeId,
                startDate,
                endDate,
                UnavailabilityReasonType.TRAINING,
                "Đào tạo kỹ năng",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0L
        );
        when(loadUnavailabilityPort.findByIdForUpdate(declarationId)).thenReturn(Optional.of(declaration));
        when(saveUnavailabilityPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee employee = new Employee(
                new EmployeeId(employeeId),
                new UserId(10L),
                1L,
                "EMP005",
                "Trần Văn E",
                "DEV",
                LocalDate.of(2022, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        YearWeek yw = YearWeek.from(startDate);

        // Đã có bản ghi phân bổ dự án 32h trong tuần này
        WeeklyProjectAllocation existingAlloc = new WeeklyProjectAllocation(
                1L, employeeId, 10L, yw, BigDecimal.valueOf(32.00));
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of(existingAlloc));

        WeeklyAvailability initialAvail = WeeklyAvailability.createCalculated(employeeId, yw, 40, 0, BigDecimal.ZERO);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yw)).thenReturn(Optional.of(initialAvail));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(loadUnavailabilityPort.findApprovedByEmployeeIdAndDateRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(declaration));

        // 1. Khi chưa confirm conflict warning -> Phải ném ngoại lệ cảnh báo xung đột (TC-02)
        ApproveUnavailabilityCommand unconfirmedCommand = new ApproveUnavailabilityCommand(declarationId, "Duyệt", false);
        assertThrows(UnavailabilityConflictException.class, () -> service.approve(unconfirmedCommand));

        // 2. Khi quản lý đã xác nhận cảnh báo xung đột (confirmConflictWarning = true)
        ApproveUnavailabilityCommand confirmedCommand = new ApproveUnavailabilityCommand(declarationId, "Duyệt dù có xung đột", true);
        UnavailabilityDeclarationResult result = service.approve(confirmedCommand);

        assertNotNull(result);
        assertEquals(UnavailabilityStatus.APPROVED, result.status());

        // Kiểm tra QTN-24: Kế hoạch phân bổ KHÔNG BỊ SỬA GIỜ (vẫn giữ nguyên 32.00 giờ)
        assertEquals(BigDecimal.valueOf(32.00), existingAlloc.getAllocatedHours());

        // Kiểm tra cờ quá tải: Khả dụng giảm còn 24h mà phân bổ 32h -> Phát sinh quá tải (isOverloaded = true)
        ArgumentCaptor<WeeklyProjectAllocation> allocCaptor = ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort).save(allocCaptor.capture());
        WeeklyProjectAllocation savedAlloc = allocCaptor.getValue();
        assertTrue(savedAlloc.isOverloaded());
        assertEquals(BigDecimal.valueOf(32.00), savedAlloc.getAllocatedHours()); // Tuyệt đối không thay đổi số giờ kế hoạch
    }

    @Test
    @DisplayName("Nhiều đơn không sẵn sàng trong cùng một tuần -> Phải cộng dồn tổng giờ của tất cả đơn đã duyệt")
    void testApproveMultipleDeclarationsInSameWeekSumsHours() {
        Long managerUserId = 99L;
        Long employeeId = 5L;
        Long declarationId2 = 102L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(managerUserId);

        User managerUser = mock(User.class);
        when(managerUser.getIdValue()).thenReturn(managerUserId);
        when(managerUser.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(managerUserId))).thenReturn(Optional.of(managerUser));

        Employee employee = new Employee(
                new EmployeeId(employeeId), new UserId(10L), 1L, "EMP005", "Trần Văn E", "DEV",
                LocalDate.of(2022, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        // Đơn 1: Thứ 2 (2026-09-21) - 8h (đã APPROVED trước đó)
        LocalDate mon = LocalDate.of(2026, 9, 21);
        UnavailabilityDeclaration decl1 = new UnavailabilityDeclaration(
                101L, employeeId, mon, mon, UnavailabilityReasonType.BUSINESS_TRIP, "Công tác 1 ngày",
                BigDecimal.valueOf(8.00), UnavailabilityStatus.APPROVED, managerUserId, "OK", null, null, null, 0L
        );

        // Đơn 2: Thứ 4 (2026-09-23) - 8h (đang được duyệt)
        LocalDate wed = LocalDate.of(2026, 9, 23);
        UnavailabilityDeclaration decl2 = new UnavailabilityDeclaration(
                declarationId2, employeeId, wed, wed, UnavailabilityReasonType.TRAINING, "Đào tạo 1 ngày",
                BigDecimal.valueOf(8.00), UnavailabilityStatus.PENDING, null, null, null, null, null, 0L
        );

        when(loadUnavailabilityPort.findByIdForUpdate(declarationId2)).thenReturn(Optional.of(decl2));
        when(saveUnavailabilityPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        YearWeek yw = YearWeek.from(mon);
        WeeklyAvailability initialAvail = WeeklyAvailability.createCalculated(employeeId, yw, 40, 0, BigDecimal.ZERO);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yw)).thenReturn(Optional.of(initialAvail));
        when(loadHolidaysPort.getHolidaysBetween(any(), any())).thenReturn(List.of());
        when(loadApprovedLeavesPort.getTotalApprovedLeaveHoursBetween(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of());

        // Cả 2 đơn đều trả về khi load các đơn APPROVED trong tuần
        when(loadUnavailabilityPort.findApprovedByEmployeeIdAndDateRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(decl1, decl2));

        ApproveUnavailabilityCommand command = new ApproveUnavailabilityCommand(declarationId2, "Duyệt đơn 2", false);
        UnavailabilityDeclarationResult result = service.approve(command);

        assertNotNull(result);
        assertEquals(UnavailabilityStatus.APPROVED, result.status());

        // Net capacity phải trừ cả 2 đơn (8h + 8h = 16h) -> 40 - 16 = 24h
        ArgumentCaptor<WeeklyAvailability> availCaptor = ArgumentCaptor.forClass(WeeklyAvailability.class);
        verify(saveWeeklyAvailabilityPort).save(availCaptor.capture());
        WeeklyAvailability savedAvail = availCaptor.getValue();
        assertEquals(BigDecimal.valueOf(24.00).setScale(2), savedAvail.getNetAvailableHours());
    }
}
