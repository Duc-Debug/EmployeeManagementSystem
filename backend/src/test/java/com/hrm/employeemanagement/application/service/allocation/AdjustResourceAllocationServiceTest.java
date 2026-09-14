package com.hrm.employeemanagement.application.service.allocation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocationChangeLogResult;
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
import com.hrm.employeemanagement.domain.allocation.AllocationChangeLog;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdjustResourceAllocationService Unit Tests (NCL-06-CN-004)")
class AdjustResourceAllocationServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;

    @Mock
    private SaveWeeklyProjectAllocationPort saveAllocationPort;

    @Mock
    private DeleteWeeklyProjectAllocationPort deleteAllocationPort;

    @Mock
    private SaveAllocationChangeLogPort saveChangeLogPort;

    @Mock
    private LoadAllocationChangeLogPort loadChangeLogPort;

    @Mock
    private SaveAuditLogPort saveAuditLogPort;

    @Mock
    private SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    @Mock
    private CheckActualHoursPort checkActualHoursPort;

    @Mock
    private AllocationNotificationPort notificationPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    private Project projectMock;

    private AdjustResourceAllocationService service;

    private final Long currentUserId = 1L;
    private final Long allocationId = 1L;
    private final Long employeeId = 100L;
    private final Long projectId = 200L;
    private final Long managerId = 300L;
    private final Long orgUnitId = 10L;
    private final int year = 2026;
    private final int weekNumber = 48; // future week

    private User currentUser;
    private Employee employee;
    private WeeklyProjectAllocation allocation;

    @BeforeEach
    void setUp() {
        service = new AdjustResourceAllocationService(
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
                saveAuditLogPort,
                deniedAuditLogPort,
                checkActualHoursPort,
                notificationPort,
                loadOrgUnitPort
        );

        Role role = new Role(new RoleId(1L), RoleCode.VT_03, "Resource Manager");
        currentUser = new User(new UserId(currentUserId), "rm_user", "pass", role, UserStatus.ACTIVE,
                new EmployeeId(currentUserId), DataScope.ORGANIZATION_BRANCH, orgUnitId, "rm@hrm.com", null, 1, 1L);

        employee = new Employee(
                new EmployeeId(employeeId), null, orgUnitId, "EMP001", "John Doe",
                "Developer", LocalDate.of(2025, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );

        allocation = new WeeklyProjectAllocation(allocationId, employeeId, projectId,
                new YearWeek(year, weekNumber), BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L);

        when(projectMock.getStatus()).thenReturn(ProjectStatus.ACTIVE);
        when(projectMock.getProjectName()).thenReturn("Project A");
        when(projectMock.getIdValue()).thenReturn(projectId);
        when(projectMock.getManagerId()).thenReturn(new EmployeeId(managerId));
    }

    private void mockAuthorizationSuccess() {
        when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(currentUserId);
        when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));
        when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);
    }

    private void setupProjectMock() {
        when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
    }

    @Nested
    @DisplayName("TC-01: Edit Hours (Sửa số giờ phân bổ)")
    class EditHoursTests {

        @Test
        @DisplayName("Should successfully adjust hours from 20h to 10h, record audit log, notify PM, and recalculate capacity")
        void shouldAdjustHoursSuccessfully() {
            mockAuthorizationSuccess();
            setupProjectMock();
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, new YearWeek(year, weekNumber)))
                    .thenReturn(Optional.of(new WeeklyAvailability(1L, employeeId, new YearWeek(year, weekNumber), 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40))));
            when(loadAllocationPort.loadAllocationsForEmployee(employeeId, new YearWeek(year, weekNumber)))
                    .thenReturn(List.of(allocation));
            when(saveAllocationPort.save(any(WeeklyProjectAllocation.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(notificationPort.notifyAllocationAdjusted(eq(projectId), eq(managerId), any())).thenReturn(String.valueOf(managerId));

            AdjustAllocationCommand command = new AdjustAllocationCommand(
                    AdjustmentAction.EDIT_HOURS,
                    BigDecimal.valueOf(10),
                    null,
                    null, null, null, null
            );

            WeeklyCapacityResult result = service.adjustAllocation(allocationId, command);

            assertNotNull(result);
            assertEquals(BigDecimal.valueOf(10), allocation.getAllocatedHours());

            // Verify change log saved with EDIT_HOURS
            ArgumentCaptor<AllocationChangeLog> logCaptor = ArgumentCaptor.forClass(AllocationChangeLog.class);
            verify(saveChangeLogPort).save(logCaptor.capture());
            AllocationChangeLog savedLog = logCaptor.getValue();
            assertEquals(AdjustmentAction.EDIT_HOURS, savedLog.getAction());
            assertTrue(savedLog.getOldValue().contains("20"));
            assertTrue(savedLog.getNewValue().contains("10"));
            assertEquals(currentUserId, savedLog.getChangedBy());

            // Verify PM notification
            verify(notificationPort).notifyAllocationAdjusted(eq(projectId), eq(managerId), any());
        }
    }

    @Nested
    @DisplayName("TC-02: Remove Allocation (Gỡ dòng phân bổ)")
    class RemoveAllocationTests {

        @Test
        @DisplayName("Should throw CannotRemoveAllocationWithActualHoursException if week ended and has actual hours")
        void shouldBlockRemovalWhenWeekEndedAndActualHoursExist() {
            WeeklyProjectAllocation pastAllocation = new WeeklyProjectAllocation(
                    allocationId, employeeId, projectId, new YearWeek(2025, 1),
                    BigDecimal.valueOf(20), BigDecimal.valueOf(50), 0L
            );
            mockAuthorizationSuccess();
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(pastAllocation));
            when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(loadProjectPort.findById(new ProjectId(projectId))).thenReturn(Optional.of(projectMock));
            when(checkActualHoursPort.hasActualHours(eq(employeeId), eq(projectId), any(YearWeek.class))).thenReturn(true);

            assertThrows(CannotRemoveAllocationWithActualHoursException.class, () -> service.removeAllocation(allocationId));
            verify(deleteAllocationPort, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should successfully remove allocation if week is in future")
        void shouldAllowRemovalWhenWeekInFuture() {
            mockAuthorizationSuccess();
            setupProjectMock();
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(checkActualHoursPort.hasActualHours(eq(employeeId), eq(projectId), any(YearWeek.class))).thenReturn(false);
            when(notificationPort.notifyAllocationAdjusted(eq(projectId), eq(managerId), any())).thenReturn(String.valueOf(managerId));

            service.removeAllocation(allocationId);

            verify(deleteAllocationPort).deleteById(allocationId);
            verify(saveChangeLogPort).save(any(AllocationChangeLog.class));
            verify(notificationPort).notifyAllocationAdjusted(eq(projectId), eq(managerId), any());
        }
    }

    @Nested
    @DisplayName("TC-03: Security & RBAC / DataScope")
    class SecurityTests {

        @Test
        @DisplayName("Should deny access when user lacks RESOURCE_ALLOCATION_MANAGE")
        void shouldDenyWhenNoPermission() {
            when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                    .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));

            AdjustAllocationCommand command = new AdjustAllocationCommand(
                    AdjustmentAction.EDIT_HOURS, BigDecimal.valueOf(10), null, null, null, null, null
            );

            assertThrows(PermissionDeniedException.class, () -> service.adjustAllocation(allocationId, command));
        }

        @Test
        @DisplayName("Should deny access and write denied audit log when user is not in the same branch as employee")
        void shouldDenyWhenBranchMismatch() {
            when(authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)).thenReturn(currentUserId);
            when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(false);

            AdjustAllocationCommand command = new AdjustAllocationCommand(
                    AdjustmentAction.EDIT_HOURS, BigDecimal.valueOf(10), null, null, null, null, null
            );

            assertThrows(PermissionDeniedException.class, () -> service.adjustAllocation(allocationId, command));
            verify(deniedAuditLogPort).save(any(AuditLog.class));
        }
    }

    @Nested
    @DisplayName("TC-04: History & Variance Note")
    class HistoryAndVarianceNoteTests {

        @Test
        @DisplayName("Should retrieve allocation change history successfully")
        void shouldRetrieveChangeHistory() {
            when(authorizationService.requireAny(PermissionCode.RESOURCE_ALLOCATION_READ, PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                    .thenReturn(currentUserId);
            when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(true);

            AllocationChangeLog changeLog = new AllocationChangeLog(
                    10L, allocationId, AdjustmentAction.EDIT_HOURS,
                    "{\"allocatedHours\":20}", "{\"allocatedHours\":10}",
                    currentUserId, LocalDateTime.now(), "300"
            );
            when(loadChangeLogPort.findByAllocationId(allocationId)).thenReturn(List.of(changeLog));
            when(loadUserPort.findAllByIdIn(any())).thenReturn(List.of(currentUser));

            List<AllocationChangeLogResult> history = service.getHistory(allocationId);

            assertNotNull(history);
            assertEquals(1, history.size());
            assertEquals(AdjustmentAction.EDIT_HOURS, history.get(0).action());
            assertEquals(currentUserId, history.get(0).changedBy());
            verify(loadUserPort).findAllByIdIn(any());
        }

        @Test
        @DisplayName("Should throw AllocationNotFoundException when getting history of non-existing allocation")
        void shouldThrowNotFoundWhenGettingHistoryOfMissingAllocation() {
            when(authorizationService.requireAny(PermissionCode.RESOURCE_ALLOCATION_READ, PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                    .thenReturn(currentUserId);
            when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));
            when(loadAllocationPort.findById(999L)).thenReturn(Optional.empty());

            assertThrows(com.hrm.employeemanagement.domain.exception.allocation.AllocationNotFoundException.class,
                    () -> service.getHistory(999L));
        }

        @Test
        @DisplayName("Should deny access when employee org unit is outside DataScope")
        void shouldDenyHistoryWhenEmployeeOutsideDataScope() {
            when(authorizationService.requireAny(PermissionCode.RESOURCE_ALLOCATION_READ, PermissionCode.RESOURCE_ALLOCATION_MANAGE))
                    .thenReturn(currentUserId);
            when(loadUserPort.findById(new UserId(currentUserId))).thenReturn(Optional.of(currentUser));
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, orgUnitId)).thenReturn(false);

            assertThrows(PermissionDeniedException.class, () -> service.getHistory(allocationId));
            verify(deniedAuditLogPort).save(any());
        }

        @Test
        @DisplayName("Should successfully record variance note")
        void shouldRecordVarianceNote() {
            mockAuthorizationSuccess();
            setupProjectMock();
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));
            when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, new YearWeek(year, weekNumber)))
                    .thenReturn(Optional.of(new WeeklyAvailability(1L, employeeId, new YearWeek(year, weekNumber), 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40))));
            when(loadAllocationPort.loadAllocationsForEmployee(employeeId, new YearWeek(year, weekNumber)))
                    .thenReturn(List.of(allocation));
            when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            WeeklyCapacityResult result = service.noteVariance(allocationId, "Khách hàng dời lịch họp");

            assertNotNull(result);
            assertEquals("Khách hàng dời lịch họp", allocation.getVarianceNote());
            verify(saveChangeLogPort).save(argThat(log -> log.getAction() == AdjustmentAction.NOTE_VARIANCE));
        }
    }

    @Nested
    @DisplayName("Move Week Tests")
    class MoveWeekTests {

        @Test
        @DisplayName("Should move allocation to future week successfully")
        void shouldMoveAllocationWeek() {
            mockAuthorizationSuccess();
            setupProjectMock();
            when(loadAllocationPort.findById(allocationId)).thenReturn(Optional.of(allocation));
            when(loadEmployeePort.findByIdForUpdate(new EmployeeId(employeeId))).thenReturn(Optional.of(employee));

            YearWeek targetWeek = new YearWeek(2026, 50);
            when(loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(employeeId, targetWeek))
                    .thenReturn(Optional.of(new WeeklyAvailability(1L, employeeId, targetWeek, 40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40))));
            when(loadAllocationPort.loadAllocationsForEmployee(employeeId, targetWeek))
                    .thenReturn(Collections.emptyList());
            when(saveAllocationPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            AdjustAllocationCommand command = new AdjustAllocationCommand(
                    AdjustmentAction.MOVE_WEEK, null, null, 2026, 50, null, null
            );

            WeeklyCapacityResult result = service.adjustAllocation(allocationId, command);

            assertNotNull(result);
            assertEquals(targetWeek, allocation.getYearWeek());
            verify(saveChangeLogPort).save(argThat(log -> log.getAction() == AdjustmentAction.MOVE_WEEK));
            verify(notificationPort).notifyAllocationAdjusted(eq(projectId), eq(managerId), any());
        }
    }
}
