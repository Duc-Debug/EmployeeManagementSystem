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
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationHoursException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceAllocationService Unit Tests")
class ResourceAllocationServiceTest {

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

    private ResourceAllocationService service;

    private final Long employeeId = 100L;
    private final Long projectId = 10L;
    private final Integer year = 2026;
    private final Integer weekNumber = 36;

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
    }

    private void setupCurrentUserWithCompanyScope() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUserMock));
        when(currentUserMock.getDataScope()).thenReturn(DataScope.COMPANY);
    }

    @Test
    @DisplayName("TC-01: Phân bổ 20h cho nhân sự có 30h rảnh -> Giờ rảnh còn lại 10h")
    void testTC01_Success_AllocationDeductsRemainingCapacity() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.valueOf(10), BigDecimal.valueOf(30));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        when(loadAllocationPort.loadAllocation(employeeId, projectId, yearWeek)).thenReturn(Optional.empty());

        WeeklyProjectAllocation savedAllocation = WeeklyProjectAllocation.createNew(employeeId, projectId, yearWeek, BigDecimal.valueOf(20));
        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenReturn(savedAllocation);

        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of(savedAllocation));

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(30), result.netAvailableHours());
        assertEquals(BigDecimal.valueOf(20), result.totalAllocatedHours());
        assertEquals(BigDecimal.valueOf(10), result.remainingAvailableHours());
        assertFalse(result.isOverAllocated());
        assertNull(result.warningMessage());

        verify(saveAllocationPort, times(1)).save(any(WeeklyProjectAllocation.class));
        verify(saveAuditLogPort, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-02: Chặn phân bổ cho nhân sự có hợp đồng lao động đã hết hạn trước tuần chọn")
    void testTC02_Exception_EmployeeContractExpired() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn B",
                "Tester", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 8, 1), false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(15));

        EmployeeInactiveException exception = assertThrows(
                EmployeeInactiveException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("kết thúc hợp đồng lao động"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Data Scope Authorization Check: Chặn phân bổ khi Nhân sự ngoài phạm vi quản lý ORGANIZATION_BRANCH")
    void testDataScopeRestriction_EmployeeOutsideBranch_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUserMock));
        when(currentUserMock.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(currentUserMock.getScopeOrgUnitId()).thenReturn(100L); // Current user manages org unit 100

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 200L, "EMP001", "Nguyễn Văn X",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

        // Employee's org unit 200 is NOT in branch of RM's scope org unit 100
        when(loadOrgUnitPort.existsInOrgUnitBranch(200L, 100L)).thenReturn(false);

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        assertThrows(
                PermissionDeniedException.class,
                () -> service.allocateResource(command)
        );

        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Blocking Issue Check: Chặn phân bổ cho Dự án không tồn tại")
    void testProjectNotFound_ThrowsException() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn C",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.empty());

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        ProjectNotFoundException exception = assertThrows(
                ProjectNotFoundException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("Không tìm thấy dự án với ID: " + projectId));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Nhập số giờ phân bổ âm -> Thông báo lỗi và không lưu DB")
    void testTC03_InvalidData_NegativeHours() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn C",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(-10));

        InvalidAllocationHoursException exception = assertThrows(
                InvalidAllocationHoursException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("không được là số âm"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Từ chối khi User không có quyền RM")
    void testTC04_Unauthorized_NonResourceManager() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        assertThrows(
                PermissionDeniedException.class,
                () -> service.allocateResource(command)
        );

        verify(loadEmployeePort, never()).findById(any());
        verify(saveAllocationPort, never()).save(any());
    }
}
