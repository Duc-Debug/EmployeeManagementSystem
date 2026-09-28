package com.hrm.employeemanagement.application.service.allocation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;

class GetProjectWeeklyAllocationsServiceTest {

    private GetProjectDetailUseCase getProjectDetailUseCase;
    private LoadWeeklyProjectAllocationPort loadAllocationPort;
    private AuthorizationService authorizationService;
    private GetProjectWeeklyAllocationsService service;

    @BeforeEach
    void setUp() {
        getProjectDetailUseCase = mock(GetProjectDetailUseCase.class);
        loadAllocationPort = mock(LoadWeeklyProjectAllocationPort.class);
        authorizationService = mock(AuthorizationService.class);
        service = new GetProjectWeeklyAllocationsService(
                getProjectDetailUseCase, loadAllocationPort, authorizationService);
    }

    @Test
    void rejectsWeek53WhenIsoYearHasOnly52Weeks() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getByProject(1L, 2025, 53, 53));

        verify(loadAllocationPort, never())
                .loadAllocationsForProjectInWeekRange(1L, 2025, 53, 53);
    }

    @Test
    void acceptsWeek53WhenIsoYearHas53Weeks() {
        when(loadAllocationPort.loadAllocationsForProjectInWeekRange(1L, 2026, 53, 53))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.getByProject(1L, 2026, 53, 53));

        verify(loadAllocationPort).loadAllocationsForProjectInWeekRange(1L, 2026, 53, 53);
    }
}
