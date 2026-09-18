package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityConflictCheckResult;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityDeclaration;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityReasonType;
import com.hrm.employeemanagement.domain.unavailability.UnavailabilityStatus;
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
    private CheckUnavailabilityConflictService service;

    @BeforeEach
    void setUp() {
        loadUnavailabilityPort = mock(LoadUnavailabilityDeclarationPort.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        authorizationService = mock(AuthorizationService.class);

        service = new CheckUnavailabilityConflictService(
                loadUnavailabilityPort,
                loadAllocationPort,
                authorizationService
        );
    }

    @Test
    @DisplayName("NCL-13-CN-003-TC-02: Phát hiện xung đột phân bổ khi có phân bổ trùng tuần khai báo")
    void testCheckConflictDetectsExistingAllocations() {
        Long declarationId = 1L;
        Long employeeId = 5L;
        LocalDate start = LocalDate.of(2026, 9, 21);
        LocalDate end = LocalDate.of(2026, 9, 22);

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(99L);

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

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(99L);

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

        YearWeek yw = YearWeek.from(start);
        when(loadAllocationPort.loadAllocationsForEmployee(eq(employeeId), eq(yw))).thenReturn(List.of());

        UnavailabilityConflictCheckResult result = service.checkConflict(declarationId);

        assertNotNull(result);
        assertFalse(result.hasConflict());
        assertEquals(0, result.conflictingAllocationsCount());
        assertNull(result.warningMessage());
    }
}
