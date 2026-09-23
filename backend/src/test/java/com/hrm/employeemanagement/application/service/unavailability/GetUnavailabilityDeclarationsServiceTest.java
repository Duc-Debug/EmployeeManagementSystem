package com.hrm.employeemanagement.application.service.unavailability;

import com.hrm.employeemanagement.application.dto.unavailability.UnavailabilityDeclarationResult;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.unavailability.LoadUnavailabilityDeclarationPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class GetUnavailabilityDeclarationsServiceTest {

    private LoadUnavailabilityDeclarationPort loadUnavailabilityPort;
    private LoadEmployeePort loadEmployeePort;
    private LoadUserPort loadUserPort;
    private LoadOrgUnitPort loadOrgUnitPort;
    private AuthorizationService authorizationService;
    private GetUnavailabilityDeclarationsService service;

    @BeforeEach
    void setUp() {
        loadUnavailabilityPort = mock(LoadUnavailabilityDeclarationPort.class);
        loadEmployeePort = mock(LoadEmployeePort.class);
        loadUserPort = mock(LoadUserPort.class);
        loadOrgUnitPort = mock(LoadOrgUnitPort.class);
        authorizationService = mock(AuthorizationService.class);

        service = new GetUnavailabilityDeclarationsService(
                loadUnavailabilityPort,
                loadEmployeePort,
                loadUserPort,
                loadOrgUnitPort,
                authorizationService
        );
    }

    @Test
    @DisplayName("Lấy danh sách chờ duyệt theo COMPANY scope khi orgUnitId = null -> Gọi findAllPending() toàn công ty")
    void testGetPendingDeclarationsCompanyScopeCallsFindAllPending() {
        Long adminUserId = 1L;
        when(authorizationService.require(PermissionCode.UNAVAILABILITY_APPROVE)).thenReturn(adminUserId);

        User adminUser = mock(User.class);
        when(adminUser.getDataScope()).thenReturn(DataScope.COMPANY);
        when(loadUserPort.findById(new UserId(adminUserId))).thenReturn(Optional.of(adminUser));

        UnavailabilityDeclaration pendingDecl = new UnavailabilityDeclaration(
                10L, 5L, LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23),
                UnavailabilityReasonType.TRAINING, "Học", BigDecimal.valueOf(16),
                UnavailabilityStatus.PENDING, null, null, null, null, null, 0L
        );
        when(loadUnavailabilityPort.findAllPending()).thenReturn(List.of(pendingDecl));

        List<UnavailabilityDeclarationResult> results = service.getPendingDeclarations(null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).id());
        verify(loadUnavailabilityPort).findAllPending();
        verify(loadUnavailabilityPort, never()).findPendingByOrgUnitIds(any());
    }

    @Test
    @DisplayName("Lấy danh sách khai báo cá nhân (getMyDeclarations) -> Trả về danh sách đơn của chính nhân viên")
    void testGetMyDeclarationsReturnsPersonalDeclarations() {
        Long currentUserId = 10L;
        Long employeeId = 5L;

        when(authorizationService.require(PermissionCode.UNAVAILABILITY_READ)).thenReturn(currentUserId);

        Employee employee = new Employee(
                new EmployeeId(employeeId), new UserId(currentUserId), 1L, "EMP005", "Nguyễn Văn A", "DEV",
                LocalDate.of(2023, 1, 1), null, false, 40, EmployeeStatus.ACTIVE
        );
        when(loadEmployeePort.findByUserId(new UserId(currentUserId))).thenReturn(Optional.of(employee));

        UnavailabilityDeclaration decl = new UnavailabilityDeclaration(
                10L, employeeId, LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23),
                UnavailabilityReasonType.TRAINING, "Học", BigDecimal.valueOf(16),
                UnavailabilityStatus.PENDING, null, null, null, null, null, 0L
        );
        when(loadUnavailabilityPort.findByEmployeeId(employeeId)).thenReturn(List.of(decl));

        List<UnavailabilityDeclarationResult> results = service.getMyDeclarations();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).id());
        verify(loadUnavailabilityPort).findByEmployeeId(employeeId);
    }
}