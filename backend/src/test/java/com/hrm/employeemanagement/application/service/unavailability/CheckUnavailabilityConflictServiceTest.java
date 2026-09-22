package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckUnavailabilityConflictServiceTest {

    private LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private AuthorizationService authorizationService;
    private LoadUserPort loadUserPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private UnavailabilityDataScopeValidator dataScopeValidator;
    private CheckUnavailabilityConflictService service;

    @BeforeEach
    void setUp() {
        loadUnavailabilityPort = mock(LoadUnavailabilityDeclarationPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        authorizationService = mock(AuthorizationService.class);
        loadUserPort = mock(LoadUserPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        dataScopeValidator = new UnavailabilityDataScopeValidator(loadOrgUnitPort);

        service = new CheckUnavailabilityConflictService(
                loadUnavailabilityPort,
                loadAllocationPort,
                authorizationService,
                loadUserPort,
                loadEmployeePort,
                dataScopeValidator
        );
    }

    private User createManagerUser(Long userId, DataScope dataScope, Long scopeOrgUnitId) {
        User user = mock(User.class);
        when(user.getIdValue()).thenReturn(userId);
        when(user.getDataScope()).thenReturn(dataScope);
        when(user.getScopeOrgUnitId()).thenReturn(scopeOrgUnitId);
        return user;
    }

    private Employee createEmployee(Long employeeId, Long userId, Long orgUnitId) {
        return new Employee(
                new EmployeeId(employeeId),
                new UserId(userId),
                orgUnitId,
                "EMP005",
                "Trần Văn E",
                "DEV",
                LocalDate.of(2022, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("NCL-13-CN-003-TC-02: Phát hiện xung đột phân bổ khi có phân bổ trùng tuần khai báo")
    void testCheckConflictDetectsExistingAllocations() {
        Long declarationId = 1L;
        Long employeeId = 5L;
        LocalDate start = LocalDate.of(2026, 9, 21);
        LocalDate end = LocalDate.of(2026, 9, 22);

        User manager = createManagerUser(99L, DataScope.COMPANY, null);
        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(99L);
        when(loadUserPort.findById(new UserId(99L))).thenReturn(Optional.of(manager));

        UnavailabilityDeclaration declaration = new UnavailabilityDeclaration(
                declarationId,
                employeeId,
                start,
                end,
                UnavailabilityReasonType.TRAINING,
                "Đào tạo",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0L
        );
        when(loadUnavailabilityPort.findById(declarationId)).thenReturn(Optional.of(declaration));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(createEmployee(employeeId, 105L, 10L)));

        YearWeek yw = YearWeek.from(start);
        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(
                10L, employeeId, 2L, yw, BigDecimal.valueOf(20.00));
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of(alloc));

        UnavailabilityConflictCheckResult result = service.checkConflict(declarationId);

        assertNotNull(result);
        assertTrue(result.hasConflict());
        assertEquals(1, result.conflictingAllocationsCount());
        assertEquals(BigDecimal.valueOf(20.00), result.totalConflictingHours());
        assertNotNull(result.warningMessage());
        assertTrue(result.warningMessage().contains("Cảnh báo"));
    }

    @Test
    @DisplayName("Không có xung đột khi không có phân bổ nào trong tuần khai báo")
    void testCheckConflictWhenNoAllocationExists() {
        Long declarationId = 1L;
        Long employeeId = 5L;
        LocalDate start = LocalDate.of(2026, 9, 21);
        LocalDate end = LocalDate.of(2026, 9, 22);

        User manager = createManagerUser(99L, DataScope.COMPANY, null);
        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(99L);
        when(loadUserPort.findById(new UserId(99L))).thenReturn(Optional.of(manager));

        UnavailabilityDeclaration declaration = new UnavailabilityDeclaration(
                declarationId,
                employeeId,
                start,
                end,
                UnavailabilityReasonType.TRAINING,
                "Đào tạo",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0L
        );
        when(loadUnavailabilityPort.findById(declarationId)).thenReturn(Optional.of(declaration));
        when(loadEmployeePort.findById(new EmployeeId(employeeId))).thenReturn(Optional.of(createEmployee(employeeId, 105L, 10L)));

        YearWeek yw = YearWeek.from(start);
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of());

        UnavailabilityConflictCheckResult result = service.checkConflict(declarationId);

        assertNotNull(result);
        assertFalse(result.hasConflict());
        assertEquals(0, result.conflictingAllocationsCount());
        assertNull(result.warningMessage());
    }

    @Test
    @DisplayName("Data Scope Blocker: Chặn người dùng kiểm tra xung đột nếu nhân viên nằm ngoài Data Scope")
    void testCheckConflictThrowsPermissionDeniedWhenEmployeeNotInScope() {
        Long declarationId = 1L;
        Long employeeId = 5L;
        Long managerUserId = 99L;
        Long managerOrgUnitId = 20L;
        Long employeeOrgUnitId = 30L;

        User manager = createManagerUser(managerUserId, DataScope.ORGANIZATION_BRANCH, managerOrgUnitId);
        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(managerUserId);
        when(loadUserPort.findById(new UserId(managerUserId))).thenReturn(Optional.of(manager));

        UnavailabilityDeclaration declaration = new UnavailabilityDeclaration(
                declarationId,
                employeeId,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.OTHER,
                "Việc riêng",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0L
        );
        when(loadUnavailabilityPort.findById(declarationId)).thenReturn(Optional.of(declaration));
        when(loadEmployeePort.findById(new EmployeeId(employeeId)))
                .thenReturn(Optional.of(createEmployee(employeeId, 105L, employeeOrgUnitId)));

        // Phòng 30 KHÔNG nằm trong nhánh phòng 20 của Quản lý
        when(loadOrgUnitPort.existsInOrgUnitBranch(employeeOrgUnitId, managerOrgUnitId)).thenReturn(false);

        assertThrows(PermissionDeniedException.class, () -> service.checkConflict(declarationId));
        verify(loadAllocationPort, never()).loadAllocationsForEmployee(anyLong(), any());
    }

    @Test
    @DisplayName("Data Scope: Cho phép kiểm tra xung đột khi nhân viên nằm trong cùng phòng ban/nhánh phòng ban")
    void testCheckConflictAllowedWhenEmployeeInScope() {
        Long declarationId = 1L;
        Long employeeId = 5L;
        Long managerUserId = 99L;
        Long managerOrgUnitId = 20L;
        Long employeeOrgUnitId = 25L;

        User manager = createManagerUser(managerUserId, DataScope.ORGANIZATION_BRANCH, managerOrgUnitId);
        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(managerUserId);
        when(loadUserPort.findById(new UserId(managerUserId))).thenReturn(Optional.of(manager));

        UnavailabilityDeclaration declaration = new UnavailabilityDeclaration(
                declarationId,
                employeeId,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.OTHER,
                "Việc riêng",
                BigDecimal.valueOf(16.00),
                UnavailabilityStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0L
        );
        when(loadUnavailabilityPort.findById(declarationId)).thenReturn(Optional.of(declaration));
        when(loadEmployeePort.findById(new EmployeeId(employeeId)))
                .thenReturn(Optional.of(createEmployee(employeeId, 105L, employeeOrgUnitId)));

        // Phòng 25 nằm trong nhánh phòng 20 của Quản lý
        when(loadOrgUnitPort.existsInOrgUnitBranch(employeeOrgUnitId, managerOrgUnitId)).thenReturn(true);
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), any())).thenReturn(List.of());

        UnavailabilityConflictCheckResult result = service.checkConflict(declarationId);
        assertNotNull(result);
        assertFalse(result.hasConflict());
    }
}
