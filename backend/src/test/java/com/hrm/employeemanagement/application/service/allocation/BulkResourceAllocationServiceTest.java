package com.hrm.employeemanagement.application.service.allocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult;
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
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.allocation.ProjectInactiveException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
@DisplayName("BulkResourceAllocationService Unit Tests (NCL-06-CN-006)")
class BulkResourceAllocationServiceTest {

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

    @Mock
    private User currentUserMock;

    private BulkResourceAllocationService service;

    private final Long employeeId = 100L;
    private final Long projectId = 10L;
    private final Long currentUserId = 1L;

    @BeforeEach
    void setUp() {
        service = new BulkResourceAllocationService(
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
    }

    private Employee createMockEmployee(EmployeeStatus status, LocalDate contractEndDate) {
        return new Employee(
                new EmployeeId(employeeId),
                null,
                1L,
                "EMP001",
                "Nguyen Van A",
                "Developer",
                LocalDate.of(2025, 1, 1),
                contractEndDate,
                false,
                40,
                status
        );
    }

    private void mockAuthAndUser() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUserMock));
        when(currentUserMock.getDataScope()).thenReturn(DataScope.COMPANY);
    }

    @Test
    @DisplayName("NCL-06-CN-006-TC-01: Phân bổ hàng loạt 12 tuần thành công trọn vẹn")
    void shouldAllocateSuccessfullyFor12WeeksWhenAllAvailable() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of());

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 12, BigDecimal.valueOf(20)
        );

        // When
        BulkAllocationResult result = service.bulkAllocateResource(command);

        // Then
        assertNotNull(result);
        assertEquals(12, result.totalRequestedWeeks());
        assertEquals(12, result.successCount());
        assertEquals(0, result.blockedCount());
        assertEquals(12, result.successWeeks().size());
        assertEquals(0, result.blockedWeeks().size());

        verify(saveAllocationPort, times(12)).save(any());
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("NCL-06-CN-006-TC-02: Phân bổ hàng loạt có 3 tuần bị quá tải, 9 tuần thành công")
    void shouldAllocatePartiallyWhen3WeeksAreOverloaded() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        // 3 tuần (tuần 3, 4, 5) đã bị phân bổ 30h cho dự án khác (với netAvailable = 40h, requested = 20h => total = 50h > 40h)
        List<WeeklyProjectAllocation> existingAllocations = List.of(
                WeeklyProjectAllocation.createNew(employeeId, 99L, YearWeek.of(2026, 3), BigDecimal.valueOf(30)),
                WeeklyProjectAllocation.createNew(employeeId, 99L, YearWeek.of(2026, 4), BigDecimal.valueOf(30)),
                WeeklyProjectAllocation.createNew(employeeId, 99L, YearWeek.of(2026, 5), BigDecimal.valueOf(30))
        );

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any()))
                .thenReturn(existingAllocations);

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 12, BigDecimal.valueOf(20)
        );

        // When
        BulkAllocationResult result = service.bulkAllocateResource(command);

        // Then
        assertNotNull(result);
        assertEquals(12, result.totalRequestedWeeks());
        assertEquals(9, result.successCount());
        assertEquals(3, result.blockedCount());
        assertEquals(9, result.successWeeks().size());
        assertEquals(3, result.blockedWeeks().size());

        // Kiểm tra 3 tuần bị chặn đúng tuần 3, 4, 5 với mã lý do CAPACITY_EXCEEDED
        assertEquals(3, result.blockedWeeks().get(0).weekNumber());
        assertEquals("CAPACITY_EXCEEDED", result.blockedWeeks().get(0).reasonCode());
        assertEquals(4, result.blockedWeeks().get(1).weekNumber());
        assertEquals("CAPACITY_EXCEEDED", result.blockedWeeks().get(1).reasonCode());
        assertEquals(5, result.blockedWeeks().get(2).weekNumber());
        assertEquals("CAPACITY_EXCEEDED", result.blockedWeeks().get(2).reasonCode());

        verify(saveAllocationPort, times(9)).save(any());
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("QTN-05: Chặn các tuần sau ngày kết thúc hợp đồng lao động")
    void shouldBlockWeeksAfterContractEndDate() {
        // Given
        mockAuthAndUser();
        // Hợp đồng kết thúc vào cuối tuần 4 năm 2026 (ngày 25/01/2026)
        LocalDate contractEnd = YearWeek.of(2026, 4).getEndDate();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, contractEnd);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of());

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 8, BigDecimal.valueOf(20)
        );

        // When
        BulkAllocationResult result = service.bulkAllocateResource(command);

        // Then
        assertEquals(8, result.totalRequestedWeeks());
        assertEquals(4, result.successCount());
        assertEquals(4, result.blockedCount()); // Tuần 5, 6, 7, 8 bị chặn do hết hạn hợp đồng
        assertEquals("CONTRACT_EXPIRED", result.blockedWeeks().get(0).reasonCode());

        verify(saveAllocationPort, times(4)).save(any());
    }

    @Test
    @DisplayName("QTN-08: Ném ProjectInactiveException khi dự án không ở trạng thái ACTIVE")
    void shouldThrowWhenProjectIsNotActive() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.CLOSED);

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 4, BigDecimal.valueOf(20)
        );

        // When & Then
        assertThrows(ProjectInactiveException.class, () -> service.bulkAllocateResource(command));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("NCL-06-CN-006-TC-03: Ném PermissionDeniedException khi người dùng không có quyền quản lý phân bổ")
    void shouldThrowWhenUserLacksPermission() {
        // Given
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 4, BigDecimal.valueOf(20)
        );

        // When & Then
        assertThrows(PermissionDeniedException.class, () -> service.bulkAllocateResource(command));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Ném EmployeeInactiveException khi nhân sự không ở trạng thái ACTIVE")
    void shouldThrowWhenEmployeeIsInactive() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.TERMINATED, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 4, BigDecimal.valueOf(20)
        );

        // When & Then
        assertThrows(EmployeeInactiveException.class, () -> service.bulkAllocateResource(command));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi khoảng tuần bắt đầu lớn hơn tuần kết thúc")
    void shouldThrowWhenFromWeekIsAfterToWeek() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 10, 2026, 5, BigDecimal.valueOf(20)
        );

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.bulkAllocateResource(command));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("HIGH-02: Ném IllegalArgumentException khi allocatedHoursPerWeek <= 0 hoặc > 168")
    void shouldThrowWhenAllocatedHoursPerWeekIsInvalid() {
        // Zero hours
        assertThrows(IllegalArgumentException.class, () -> new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 4, BigDecimal.ZERO
        ));

        // Negative hours
        assertThrows(IllegalArgumentException.class, () -> new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 4, BigDecimal.valueOf(-10)
        ));

        // Excessive hours > 168
        assertThrows(IllegalArgumentException.class, () -> new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 4, BigDecimal.valueOf(200)
        ));
    }

    @Test
    @DisplayName("MEDIUM-01: BlockedWeekSummary trả về đúng currentTotalAllocatedHours của tất cả dự án hiện tại")
    void shouldReturnCorrectCurrentTotalAllocatedHoursWhenBlocked() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yw = YearWeek.of(2026, 1);
        // Project khác (ID 99) đang chiếm 30h
        WeeklyProjectAllocation otherAlloc = WeeklyProjectAllocation.createNew(employeeId, 99L, yw, BigDecimal.valueOf(30));
        // Chính project hiện tại (ID 10) đang có 5h
        WeeklyProjectAllocation currentProjAlloc = WeeklyProjectAllocation.createNew(employeeId, projectId, yw, BigDecimal.valueOf(5));

        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of(otherAlloc, currentProjAlloc));

        // Yêu cầu phân bổ thêm 20h cho project 10 -> otherProjectsSum (30h) + 20h = 50h > 40h -> Blocked!
        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 1, BigDecimal.valueOf(20)
        );

        // When
        BulkAllocationResult result = service.bulkAllocateResource(command);

        // Then
        assertEquals(0, result.successCount());
        assertEquals(1, result.blockedCount());
        BulkAllocationResult.BlockedWeekSummary blocked = result.blockedWeeks().get(0);
        assertEquals("CAPACITY_EXCEEDED", blocked.reasonCode());
        // currentAllocatedHours phải là tổng hiện tại của cả 2 dự án (30 + 5 = 35h)
        assertEquals(BigDecimal.valueOf(35), blocked.currentAllocatedHours());
        assertEquals(BigDecimal.valueOf(40), blocked.netAvailableHours());
        assertEquals(BigDecimal.valueOf(20), blocked.requestedHours());
    }

    @Test
    @DisplayName("NCL-06-CN-007: Phân bổ hàng loạt theo tỷ lệ 50% tự động tính giờ theo từng tuần có độ rảnh khác nhau")
    void shouldAllocateSuccessfullyWithPercentageAcrossWeeksWithVaryingAvailability() {
        // Given
        mockAuthAndUser();
        Employee employee = createMockEmployee(EmployeeStatus.ACTIVE, null);
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        // Tuần 1: 40h khả dụng; Tuần 2: 32h khả dụng (nghỉ lễ 8h)
        YearWeek w1 = YearWeek.of(2026, 1);
        YearWeek w2 = YearWeek.of(2026, 2);
        WeeklyAvailability availW1 = new WeeklyAvailability(1L, employeeId, w1, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        WeeklyAvailability availW2 = new WeeklyAvailability(2L, employeeId, w2, 40, 0, BigDecimal.valueOf(8), BigDecimal.valueOf(32));

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of(availW1, availW2));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any()))
                .thenReturn(List.of());

        // Phân bổ 50% mỗi tuần
        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                employeeId, projectId, 2026, 1, 2026, 2, null, BigDecimal.valueOf(50)
        );

        // When
        BulkAllocationResult result = service.bulkAllocateResource(command);

        // Then
        assertNotNull(result);
        assertEquals(2, result.totalRequestedWeeks());
        assertEquals(2, result.successCount());
        assertEquals(0, result.blockedCount());

        // Tuần 1: 50% của 40h = 20.00h; Tuần 2: 50% của 32h = 16.00h
        assertEquals(0, new BigDecimal("20.00").compareTo(result.successWeeks().get(0).allocatedHours()));
        assertEquals(0, new BigDecimal("20.00").compareTo(result.successWeeks().get(0).remainingHours()));

        assertEquals(0, new BigDecimal("16.00").compareTo(result.successWeeks().get(1).allocatedHours()));
        assertEquals(0, new BigDecimal("16.00").compareTo(result.successWeeks().get(1).remainingHours()));

        org.mockito.ArgumentCaptor<WeeklyProjectAllocation> captor = org.mockito.ArgumentCaptor.forClass(WeeklyProjectAllocation.class);
        verify(saveAllocationPort, times(2)).save(captor.capture());

        List<WeeklyProjectAllocation> saved = captor.getAllValues();
        assertEquals(0, new BigDecimal("20.00").compareTo(saved.get(0).getAllocatedHours()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(saved.get(0).getAllocationPercentage()));

        assertEquals(0, new BigDecimal("16.00").compareTo(saved.get(1).getAllocatedHours()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(saved.get(1).getAllocationPercentage()));
    }
}
