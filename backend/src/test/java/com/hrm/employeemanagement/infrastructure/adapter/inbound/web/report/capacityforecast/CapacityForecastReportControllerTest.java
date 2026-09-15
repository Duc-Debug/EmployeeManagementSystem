package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.capacityforecast;

import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult.*;
import com.hrm.employeemanagement.application.port.inbound.report.capacityforecast.GetCapacityForecastUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CapacityForecastReportController Web Adapter Tests (NCL-10-CN-004)")
class CapacityForecastReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetCapacityForecastUseCase getCapacityForecastUseCase;

    @InjectMocks
    private CapacityForecastReportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API - Lấy báo cáo dự báo năng lực thành công (HTTP 200 OK)")
    void getCapacityForecastReport_Success() throws Exception {
        WeeklyForecastItem week1 = new WeeklyForecastItem(
                2026, 38, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 20),
                BigDecimal.valueOf(400.0), BigDecimal.valueOf(300.0), BigDecimal.valueOf(40.0),
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(60.0),
                BigDecimal.valueOf(75.0), BigDecimal.valueOf(85.0),
                ForecastStatus.NEAR_FULL
        );

        CapacityForecastSummary summary = new CapacityForecastSummary(
                BigDecimal.valueOf(4800.0), BigDecimal.valueOf(3600.0), BigDecimal.valueOf(480.0),
                BigDecimal.valueOf(720.0), 0
        );

        CapacityForecastResult mockResult = new CapacityForecastResult(
                10L, "Delivery", 2026, 38, 12, List.of(week1), summary, LocalDateTime.now()
        );

        when(getCapacityForecastUseCase.execute(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/reports/capacity-forecast")
                        .param("orgUnitId", "10")
                        .param("fromYear", "2026")
                        .param("fromWeek", "38")
                        .param("durationWeeks", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orgUnitId").value(10))
                .andExpect(jsonPath("$.data.orgUnitName").value("Delivery"))
                .andExpect(jsonPath("$.data.weeks[0].year").value(2026))
                .andExpect(jsonPath("$.data.weeks[0].weekNumber").value(38))
                .andExpect(jsonPath("$.data.weeks[0].availableHours").value(400.0))
                .andExpect(jsonPath("$.data.weeks[0].committedHours").value(300.0))
                .andExpect(jsonPath("$.data.weeks[0].reservedHours").value(40.0))
                .andExpect(jsonPath("$.data.weeks[0].projectedRemainingHours").value(60.0))
                .andExpect(jsonPath("$.data.summary.totalAvailableHours").value(4800.0));
    }

    @Test
    @DisplayName("API - Người dùng không đủ quyền nhận HTTP 403 Forbidden")
    void getCapacityForecastReport_Forbidden() throws Exception {
        when(getCapacityForecastUseCase.execute(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.CAPACITY_FORECAST_REPORT_READ));

        mockMvc.perform(get("/api/v1/reports/capacity-forecast"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
