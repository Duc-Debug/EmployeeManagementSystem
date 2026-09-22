package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.BulkAllocationResult;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.AllocationNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.CheckActualHoursPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.DeleteWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadAllocationChangeLogPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveAllocationChangeLogPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.AdjustmentAction;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.OutsourcedContractPeriodException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NCL-14-CN-002: Outsourced Resource Allocation Service Tests (QTN-21)")
class OutsourcedResourceAllocationServiceTest {

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
    private SaveAuditLogPort commonSaveAuditLogPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private SaveAllocationChangeLogPort saveChangeLogPort;

    @Mock
    private LoadAllocationChangeLogPort loadChangeLogPort;

    @Mock
    private AllocationNotificationPort notificationPort;

    @Mock
    private CheckActualHoursPort checkActualHoursPort;

    @Mock
    private DeleteWeeklyProjectAllocationPort deleteAllocationPort;

    private ResourceAllocationService resourceAllocationService;
    private BulkResourceAllocationService bulkResourceAllocationService;
    private AdjustResourceAllocationService adjustResourceAllocationService;

    private final Long rmUserId = 1L;
    private final Long orgUnitId = 10L;
    private final Long projectId = 100L;
    private final Long outsourcedEmpId = 500L;

    // Hợp đồng thuê ngoài: từ 01/06/2027 (Tuần 22) đến 31/08/2027 (Tuần 35)
    private final LocalDate contractStart = LocalDate.of(2027, 6, 1);
    private final LocalDate contractEnd = LocalDate.of(2027, 8, 31);
    private final String providerName = "FPT Software Outsourcing";

    private Employee outsourcedEmployee;
    private Project activeProject;
    private User rmUser;

    @BeforeEach
    void setUp() {
        resourceAllocationService = new ResourceAllocationService(
                authorizationService,
                loadEmployeePort,
                loadProjectPort,
                loadWeeklyAvailabilityPort,
                saveAllocationPort,
                loadAllocationPort,
                saveAuditLogPort,
                loadUserPort,
                loadOrgUnitPort,
                saveChangeLogPort,
                notificationPort,
                null
        );

        bulkResourceAllocationService = new BulkResourceAllocationService(
                authorizationService,
                loadEmployeePort,
                loadProjectPort,
                loadWeeklyAvailabilityPort,
                saveAllocationPort,
                loadAllocationPort,
                saveAuditLogPort,
                loadUserPort,
                loadOrgUnitPort,
                saveChangeLogPort,
                notificationPort
        );

        adjustResourceAllocationService = new AdjustResourceAllocationService(
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                loadProjectPort,
                loadWeeklyAvailabilityPort,
                loadAllocationPort,
                saveAllocationPort,
                deleteAllocationPort,
                saveChangeLogPort,
                loadChangeLogPort,
                commonSaveAuditLogPort,
                saveAuditLogPort,
                checkActualHoursPort,
                notificationPort,
                loadOrgUnitPort
        );

        outsourcedEmployee = new Employee(
                new EmployeeId(outsourcedEmpId),
                null,
                orgUnitId,
                "OS-001",
                "Chuyên gia DevOps",
                "Senior DevOps",
                contractStart,
                contractEnd,
                true,
                40,
                EmployeeStatus.ACTIVE,
                providerName,
                1L
        );

        activeProject = new Project(
                new ProjectId(projectId),
                "PRJ-2027",
                "Hệ thống E-Commerce",
                orgUnitId,
                new EmployeeId(2L),
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 12, 31),
                BigDecimal.valueOf(1000),
                "Mô tả dự án",
                ProjectStatus.ACTIVE,
                new UserId(1L),
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L
        );

        Role rmRole = new Role(new RoleId(3L), RoleCode.VT_03, "Quản lý nguồn lực");
        rmUser = new User(
                new UserId(rmUserId),
                "rm_manager",
                "passwordHash",
                rmRole,
                UserStatus.ACTIVE,
                new EmployeeId(rmUserId),
                DataScope.ORGANIZATION_BRANCH,
                orgUnitId,
                "rm@hrm.com",
                null,
                1,
                1L
        );
    }

    @Test
    @DisplayName("NCL-14-CN-002-TC-01: Phân bổ thành công nhân sự thuê ngoài vào tuần nằm trong thời hạn hợp đồng")
    void shouldAllocateOutsourcedEmployeeSuccessfullyWithinContractPeriod() {
        // Tuần 26/2027: nằm trọn trong hạn hợp đồng (01/06/2027 - 31/08/2027)
        int year = 2027;
        int weekNumber = 26;
        YearWeek yearWeek = YearWeek.of(year, weekNumber);

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(outsourcedEmpId))).thenReturn(Optional.of(outsourcedEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(activeProject));

        WeeklyAvailability availability = new WeeklyAvailability(
                1L, outsourcedEmpId, yearWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40)
        );
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(outsourcedEmpId, yearWeek))
                .thenReturn(Optional.of(availability));

        WeeklyProjectAllocation savedAlloc = new WeeklyProjectAllocation(
                999L, outsourcedEmpId, projectId, yearWeek, BigDecimal.valueOf(40), BigDecimal.valueOf(100), 0L
        );
        when(loadAllocationPort.loadAllocationsForEmployee(outsourcedEmpId, yearWeek))
                .thenReturn(List.of())
                .thenReturn(List.of(savedAlloc));

        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenAnswer(inv -> savedAlloc);

        AllocateResourceCommand command = new AllocateResourceCommand(
                outsourcedEmpId, projectId, year, weekNumber, BigDecimal.valueOf(40)
        );

        WeeklyCapacityResult result = resourceAllocationService.allocateResource(command);

        assertNotNull(result);
        assertEquals(outsourcedEmpId, result.employeeId());
        assertEquals(year, result.year());
        assertEquals(weekNumber, result.weekNumber());
        assertEquals(BigDecimal.valueOf(40), result.totalAllocatedHours());
        assertFalse(result.isOverAllocated());

        verify(saveAllocationPort).save(any(WeeklyProjectAllocation.class));
        verify(saveAuditLogPort).save(any());
    }

    @Test
    @DisplayName("NCL-14-CN-002-TC-02: Chặn phân bổ khi tuần nằm trước ngày bắt đầu hợp đồng thuê ngoài (QTN-21)")
    void shouldThrowExceptionWhenAllocatingBeforeOutsourcedContractStartDate() {
        // Tuần 18/2027: tháng 5/2027 kết thúc trước 01/06/2027
        int year = 2027;
        int weekNumber = 18;

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(outsourcedEmpId))).thenReturn(Optional.of(outsourcedEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);

        AllocateResourceCommand command = new AllocateResourceCommand(
                outsourcedEmpId, projectId, year, weekNumber, BigDecimal.valueOf(20)
        );

        OutsourcedContractPeriodException ex = assertThrows(
                OutsourcedContractPeriodException.class,
                () -> resourceAllocationService.allocateResource(command)
        );

        assertTrue(ex.getMessage().contains("chưa có hiệu lực"));
        assertTrue(ex.getMessage().contains(providerName));
        assertEquals(outsourcedEmpId, ex.getEmployeeId());
        assertEquals(contractStart, ex.getStartDate());
    }

    @Test
    @DisplayName("NCL-14-CN-002-TC-03: Chặn phân bổ khi tuần nằm sau ngày kết thúc hợp đồng thuê ngoài (QTN-21)")
    void shouldThrowExceptionWhenAllocatingAfterOutsourcedContractEndDate() {
        // Tuần 40/2027: tháng 10/2027 bắt đầu sau 31/08/2027
        int year = 2027;
        int weekNumber = 40;

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(outsourcedEmpId))).thenReturn(Optional.of(outsourcedEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);

        AllocateResourceCommand command = new AllocateResourceCommand(
                outsourcedEmpId, projectId, year, weekNumber, BigDecimal.valueOf(20)
        );

        OutsourcedContractPeriodException ex = assertThrows(
                OutsourcedContractPeriodException.class,
                () -> resourceAllocationService.allocateResource(command)
        );

        assertTrue(ex.getMessage().contains("đã hết hạn"));
        assertTrue(ex.getMessage().contains(providerName));
        assertEquals(outsourcedEmpId, ex.getEmployeeId());
        assertEquals(contractEnd, ex.getContractEndDate());
    }

    @Test
    @DisplayName("NCL-14-CN-002-TC-04: Phân bổ hàng loạt tự động chặn các tuần ngoài hạn hợp đồng của nhân sự thuê ngoài")
    void shouldBlockWeeksOutsideContractInBulkAllocation() {
        // Phân bổ từ tuần 20 đến tuần 24/2027 (Hợp đồng bắt đầu từ tuần 22: 01/06/2027)
        // Tuần 20 và 21 phải bị chặn bởi CONTRACT_NOT_STARTED; Tuần 22, 23, 24 thành công
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(outsourcedEmpId))).thenReturn(Optional.of(outsourcedEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(activeProject));

        when(loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(any(), any())).thenReturn(List.of());
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(any(), any())).thenReturn(List.of());

        when(saveAllocationPort.save(any())).thenAnswer(invocation -> {
            WeeklyProjectAllocation a = invocation.getArgument(0);
            return new WeeklyProjectAllocation(
                    1000L, a.getEmployeeId(), a.getProjectId(), a.getYearWeek(), a.getAllocatedHours(), a.getAllocationPercentage(), 0L
            );
        });

        BulkAllocateResourceCommand command = new BulkAllocateResourceCommand(
                outsourcedEmpId, projectId, null, 2027, 20, 2027, 24, BigDecimal.valueOf(20), null
        );

        BulkAllocationResult result = bulkResourceAllocationService.bulkAllocateResource(command);

        assertNotNull(result);
        assertEquals(3, result.successWeeks().size()); // Tuần 22, 23, 24
        assertEquals(2, result.blockedWeeks().size()); // Tuần 20, 21

        assertTrue(result.blockedWeeks().stream().anyMatch(b -> b.weekNumber() == 20 && "CONTRACT_NOT_STARTED".equals(b.reasonCode())));
        assertTrue(result.blockedWeeks().stream().anyMatch(b -> b.weekNumber() == 21 && "CONTRACT_NOT_STARTED".equals(b.reasonCode())));
    }

    @Test
    @DisplayName("NCL-14-CN-002-TC-05: Chuyển tuần (MOVE_WEEK) sang tuần ngoài hạn hợp đồng bị chặn lỗi")
    void shouldThrowExceptionWhenMovingAllocationOutsideContractPeriod() {
        // Phân bổ hiện tại ở tuần 26/2027 (trong hạn)
        YearWeek currentWeek = YearWeek.of(2027, 26);
        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                888L, outsourcedEmpId, projectId, currentWeek, BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadAllocationPort.findById(888L)).thenReturn(Optional.of(existingAllocation));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(outsourcedEmpId))).thenReturn(Optional.of(outsourcedEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(activeProject));

        // Thử dời sang tuần 38/2027 (tháng 9/2027 - ngoài hạn hợp đồng)
        AdjustAllocationCommand command = new AdjustAllocationCommand(
                AdjustmentAction.MOVE_WEEK, null, null, 2027, 38, null, null
        );

        OutsourcedContractPeriodException ex = assertThrows(
                OutsourcedContractPeriodException.class,
                () -> adjustResourceAllocationService.adjustAllocation(888L, command)
        );

        assertTrue(ex.getMessage().contains("nằm ngoài thời hạn hợp đồng"));
        assertTrue(ex.getMessage().contains(providerName));
    }

    @Test
    @DisplayName("NCL-14-CN-002-TC-06: Chuyển tuần (MOVE_WEEK) sang tuần hợp lệ trong thời hạn hợp đồng thành công")
    void shouldMoveAllocationSuccessfullyWithinContractPeriod() {
        YearWeek currentWeek = YearWeek.of(2027, 26);
        WeeklyProjectAllocation existingAllocation = new WeeklyProjectAllocation(
                888L, outsourcedEmpId, projectId, currentWeek, BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
        );

        YearWeek targetWeek = YearWeek.of(2027, 28); // Tháng 7/2027 - nằm trong hạn hợp đồng

        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(rmUserId);
        when(loadUserPort.findById(new UserId(rmUserId))).thenReturn(Optional.of(rmUser));
        when(loadAllocationPort.findById(888L)).thenReturn(Optional.of(existingAllocation));
        when(loadEmployeePort.findByIdForUpdate(new EmployeeId(outsourcedEmpId))).thenReturn(Optional.of(outsourcedEmployee));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(activeProject));

        when(checkActualHoursPort.hasActualHours(outsourcedEmpId, projectId, currentWeek)).thenReturn(false);
        when(loadAllocationPort.loadAllocation(outsourcedEmpId, projectId, targetWeek)).thenReturn(Optional.empty());

        WeeklyAvailability targetAvail = new WeeklyAvailability(
                2L, outsourcedEmpId, targetWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40)
        );
        when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(outsourcedEmpId, targetWeek)).thenReturn(Optional.of(targetAvail));
        when(loadAllocationPort.loadAllocationsForEmployee(outsourcedEmpId, targetWeek)).thenReturn(List.of());

        when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

        AdjustAllocationCommand command = new AdjustAllocationCommand(
                AdjustmentAction.MOVE_WEEK, null, null, 2027, 28, null, null
        );

        WeeklyCapacityResult result = adjustResourceAllocationService.adjustAllocation(888L, command);

        assertNotNull(result);
        assertEquals(outsourcedEmpId, result.employeeId());
        verify(saveAllocationPort).save(any(WeeklyProjectAllocation.class));
        verify(commonSaveAuditLogPort).save(any());
    }
}
