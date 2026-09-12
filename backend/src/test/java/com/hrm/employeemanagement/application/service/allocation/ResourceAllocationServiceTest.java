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
import com.hrm.employeemanagement.domain.exception.allocation.AllocationCapacityExceededException;
import com.hrm.employeemanagement.domain.exception.allocation.EmployeeInactiveException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationHoursException;
import com.hrm.employeemanagement.domain.exception.allocation.ProjectInactiveException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
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
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.valueOf(10), BigDecimal.valueOf(30));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation savedAllocation = WeeklyProjectAllocation.createNew(employeeId, projectId, yearWeek, BigDecimal.valueOf(20));
        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenReturn(savedAllocation);

        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of())
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
    @DisplayName("Capacity Enforcement: Phân bổ vượt quá netAvailableHours -> Ném lỗi AllocationCapacityExceededException")
    void testAllocationExceedingCapacity_ThrowsAllocationCapacityExceededException() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Employee already allocated 30h to another project (ID: 99L)
        WeeklyProjectAllocation existingProjAllocation = new WeeklyProjectAllocation(1L, employeeId, 99L, yearWeek, BigDecimal.valueOf(30), 0L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of(existingProjAllocation));

        // Request 20h for new project -> Total would be 50h > 40h netAvailable
        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        AllocationCapacityExceededException exception = assertThrows(
                AllocationCapacityExceededException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("vượt quá số giờ khả dụng"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Update existing allocation: Cập nhật phân bổ từ 10h lên 20h trên cùng dự án thành công")
    void testUpdateExistingAllocation_Success() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Existing allocation on SAME project is 10h
        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(1L, employeeId, projectId, yearWeek, BigDecimal.valueOf(10), 0L);
        WeeklyProjectAllocation updatedAllocation = new WeeklyProjectAllocation(1L, employeeId, projectId, yearWeek, BigDecimal.valueOf(20), 1L);
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of(existingAllocation))
                .thenReturn(List.of(updatedAllocation));

        when(saveAllocationPort.save(any())).thenReturn(updatedAllocation);

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(20), result.remainingAvailableHours());
        verify(saveAllocationPort, times(1)).save(any());
    }

    @Test
    @DisplayName("TC-02: Chặn phân bổ cho nhân sự có hợp đồng lao động đã hết hạn trước tuần chọn")
    void testTC02_Exception_EmployeeContractExpired() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn B",
                "Tester", LocalDate.of(2025, 1, 1), LocalDate.of(2026, 8, 1), false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

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
        when(currentUserMock.getScopeOrgUnitId()).thenReturn(100L);

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 200L, "EMP001", "Nguyễn Văn X",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
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
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
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
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(-10));

        InvalidAllocationHoursException exception = assertThrows(
                InvalidAllocationHoursException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("không được là số âm"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Project Lifecycle Check: Chặn phân bổ cho Dự án ở trạng thái CLOSED")
    void testAllocationToClosedProject_ThrowsProjectInactiveException() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.CLOSED);

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        ProjectInactiveException exception = assertThrows(
                ProjectInactiveException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("không ở trạng thái hoạt động"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("Project Lifecycle Check: Chặn phân bổ cho Dự án ở trạng thái INACTIVE")
    void testAllocationToInactiveProject_ThrowsProjectInactiveException() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.INACTIVE);

        AllocateResourceCommand command = new AllocateResourceCommand(employeeId, projectId, year, weekNumber, BigDecimal.valueOf(20));

        ProjectInactiveException exception = assertThrows(
                ProjectInactiveException.class,
                () -> service.allocateResource(command)
        );

        assertTrue(exception.getMessage().contains("không ở trạng thái hoạt động"));
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("DTO Invariant Check: null cả allocatedHours và allocationPercentage ném lỗi IllegalArgumentException")
    void testNullAllocatedHours_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AllocateResourceCommand(employeeId, projectId, year, weekNumber, null)
        );
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

        verify(loadEmployeePort, never()).findByIdForUpdate(any());
        verify(saveAllocationPort, never()).save(any());
    }

    @Test
    @DisplayName("getWeeklyCapacities: Ném lỗi EmployeeNotFoundException khi nhân sự không tồn tại")
    void testGetWeeklyCapacities_EmployeeNotFound_ThrowsException() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUserMock));
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(999L)))).thenReturn(List.of());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> service.getWeeklyCapacities(List.of(999L), year, weekNumber)
        );
    }

    @Test
    @DisplayName("getWeeklyCapacities: Ném lỗi PermissionDeniedException khi nhân sự ngoài phạm vi Data Scope")
    void testGetWeeklyCapacities_EmployeeOutsideDataScope_ThrowsPermissionDenied() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUserMock));
        when(currentUserMock.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(currentUserMock.getScopeOrgUnitId()).thenReturn(100L);

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 200L, "EMP001", "Nguyễn Văn X",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(employeeId)))).thenReturn(List.of(employee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(200L, 100L)).thenReturn(false);

        assertThrows(
                PermissionDeniedException.class,
                () -> service.getWeeklyCapacities(List.of(employeeId), year, weekNumber)
        );
    }

    @Test
    @DisplayName("getWeeklyCapacities: Batch loading tối ưu (O(3) queries) trả về danh sách capacity của nhân sự trong Data Scope")
    void testGetWeeklyCapacities_Success_BatchLoading() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ)).thenReturn(1L);
        when(loadUserPort.findById(new UserId(1L))).thenReturn(Optional.of(currentUserMock));
        when(currentUserMock.getDataScope()).thenReturn(DataScope.COMPANY);

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findAllByIdIn(List.of(new EmployeeId(employeeId)))).thenReturn(List.of(employee));

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        when(loadWeeklyAvailabilityPort.findByEmployeeIdInAndYearWeek(List.of(employeeId), yearWeek)).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesInWeekRange(List.of(employeeId), year, weekNumber, weekNumber)).thenReturn(List.of());

        List<WeeklyCapacityResult> results = service.getWeeklyCapacities(List.of(employeeId), year, weekNumber);

        assertEquals(1, results.size());
        assertEquals(employeeId, results.get(0).employeeId());
        assertEquals(BigDecimal.valueOf(40), results.get(0).remainingAvailableHours());
    }

    @Test
    @DisplayName("NCL-06-CN-007 TC-01: Tuần 40h khả dụng -> Phân bổ 50% -> Quy đổi 20h và lưu cả 20h và 50%")
    void testNCL06CN007_TC01_StandardWeek40Hours_Allocate50Percent() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn A",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation savedAllocation = WeeklyProjectAllocation.createNew(
                employeeId, projectId, yearWeek, BigDecimal.valueOf(20).setScale(2), BigDecimal.valueOf(50).setScale(2));
        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenReturn(savedAllocation);

        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of())
                .thenReturn(List.of(savedAllocation));

        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectId, year, weekNumber, null, BigDecimal.valueOf(50));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(40), result.netAvailableHours());
        assertEquals(BigDecimal.valueOf(20).setScale(2), result.totalAllocatedHours());
        assertEquals(BigDecimal.valueOf(20).setScale(2), result.remainingAvailableHours());

        verify(saveAllocationPort).save(argThat(alloc ->
                alloc.getAllocatedHours().compareTo(BigDecimal.valueOf(20)) == 0 &&
                alloc.getAllocationPercentage().compareTo(BigDecimal.valueOf(50)) == 0
        ));
    }

    @Test
    @DisplayName("NCL-06-CN-007 TC-02: Tuần có ngày lễ chỉ còn 32h khả dụng -> Phân bổ 50% -> Quy đổi 16h chứ không phải 20h")
    void testNCL06CN007_TC02_HolidayWeek32Hours_Allocate50Percent() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn B",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        // Tuần có 1 ngày lễ (8h nghỉ lễ), khả dụng còn 32h
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 8, BigDecimal.ZERO, BigDecimal.valueOf(32));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation savedAllocation = WeeklyProjectAllocation.createNew(
                employeeId, projectId, yearWeek, BigDecimal.valueOf(16).setScale(2), BigDecimal.valueOf(50).setScale(2));
        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenReturn(savedAllocation);

        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of())
                .thenReturn(List.of(savedAllocation));

        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectId, year, weekNumber, null, BigDecimal.valueOf(50));

        WeeklyCapacityResult result = service.allocateResource(command);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(32), result.netAvailableHours());
        assertEquals(BigDecimal.valueOf(16).setScale(2), result.totalAllocatedHours());
        assertEquals(BigDecimal.valueOf(16).setScale(2), result.remainingAvailableHours());

        verify(saveAllocationPort).save(argThat(alloc ->
                alloc.getAllocatedHours().compareTo(BigDecimal.valueOf(16)) == 0 &&
                alloc.getAllocationPercentage().compareTo(BigDecimal.valueOf(50)) == 0
        ));
    }

    @Test
    @DisplayName("NCL-06-CN-007 TC-04: Ghi Audit Log chi tiết khi phân bổ theo phần trăm")
    void testNCL06CN007_TC04_AuditLogRecordedWithPercentage() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn C",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        WeeklyProjectAllocation savedAllocation = new WeeklyProjectAllocation(
                555L, employeeId, projectId, yearWeek, BigDecimal.valueOf(20).setScale(2), BigDecimal.valueOf(50).setScale(2), 1L);
        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenReturn(savedAllocation);

        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek))
                .thenReturn(List.of())
                .thenReturn(List.of(savedAllocation));

        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectId, year, weekNumber, null, BigDecimal.valueOf(50));

        service.allocateResource(command);

        verify(saveAuditLogPort).save(argThat(audit ->
                "RESOURCE_ALLOCATED".equals(audit.getAction()) &&
                "weekly_project_allocations".equals(audit.getTableName()) &&
                audit.getNewValue().contains("20.00h (50%)")
        ));
    }

    @Test
    @DisplayName("NCL-06-CN-007 QTN-11: Phân bổ % vượt quá giờ khả dụng còn lại ném ngoại lệ AllocationCapacityExceededException")
    void testNCL06CN007_QTN11_PercentageExceedsCapacity_ThrowsException() {
        setupCurrentUserWithCompanyScope();

        Employee employee = new Employee(
                new EmployeeId(employeeId), null, 1L, "EMP001", "Nguyễn Văn D",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
        when(projectMock.getOrgUnitId()).thenReturn(1L);
        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);

        YearWeek yearWeek = YearWeek.of(year, weekNumber);
        WeeklyAvailability availability = new WeeklyAvailability(1L, employeeId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40));
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, yearWeek)).thenReturn(Optional.of(availability));

        // Đã phân bổ 25h cho dự án khác (project 99L)
        WeeklyProjectAllocation otherProjectAlloc = WeeklyProjectAllocation.createNew(
                employeeId, 99L, yearWeek, BigDecimal.valueOf(25));
        when(loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)).thenReturn(List.of(otherProjectAlloc));

        // Yêu cầu phân bổ thêm 50% (20h) cho dự án projectId (10L) -> 25h + 20h = 45h > 40h
        AllocateResourceCommand command = new AllocateResourceCommand(
                employeeId, projectId, year, weekNumber, null, BigDecimal.valueOf(50));

        assertThrows(
                AllocationCapacityExceededException.class,
                () -> service.allocateResource(command)
        );

        verify(saveAllocationPort, never()).save(any());
    }
}
