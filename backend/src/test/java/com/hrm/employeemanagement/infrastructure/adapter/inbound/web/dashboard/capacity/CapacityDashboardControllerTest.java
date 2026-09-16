package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.dashboard.capacity;

import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardQuery;
import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardResult;
import com.hrm.employeemanagement.application.port.inbound.dashboard.capacity.GetCapacityDashboardUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapacityDashboardControllerTest {

    @Mock
    private GetCapacityDashboardUseCase useCase;

    private CapacityDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new CapacityDashboardController(useCase);
    }

    @Test
    @DisplayName("REST Controller: GET /api/v1/capacity-dashboard -> 200 OK với kết quả trả về")
    void shouldReturnCapacityDashboardResult_Success() {
        // Given
        CapacityDashboardResult mockResult = new CapacityDashboardResult(
                null, "Toàn công ty", 2026, 38, 8,
                BigDecimal.valueOf(85.5), 2, BigDecimal.valueOf(120.0), 1, 4,
                BigDecimal.valueOf(800.0), BigDecimal.valueOf(684.0),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                LocalDateTime.now()
        );
        when(useCase.execute(any(CapacityDashboardQuery.class))).thenReturn(mockResult);

        // When
        ResponseEntity<?> response = controller.getCapacityDashboard(null, 2026, 38, 8);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(useCase).execute(any(CapacityDashboardQuery.class));
    }
}
