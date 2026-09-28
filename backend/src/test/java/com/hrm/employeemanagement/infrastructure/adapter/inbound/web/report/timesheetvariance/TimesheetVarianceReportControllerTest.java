package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.timesheetvariance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceItem;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceResult;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceSummary;
import com.hrm.employeemanagement.application.port.inbound.report.timesheetvariance.GetTimesheetVarianceUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimesheetVarianceReportController Tests (NCL-09-CN-004)")
class TimesheetVarianceReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetTimesheetVarianceUseCase getTimesheetVarianceUseCase;

    @InjectMocks
    private TimesheetVarianceReportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("TC-01: API Đối chiếu giờ phân bổ vs thực tế trả về HTTP 200 OK và dữ liệu chênh lệch")
    void getTimesheetVarianceReport_Success() throws Exception {
        TimesheetVarianceItem item = new TimesheetVarianceItem(
                1L, "EMP001", "Nguyen Van A", 10L, "Phòng Kỹ thuật",
                101L, "Hệ thống HRM", 2026, 35,
                LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30),
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(38.0),
                BigDecimal.valueOf(8.0), BigDecimal.valueOf(26.7),
                true, "POSITIVE_VARIANCE"
        );

        TimesheetVarianceSummary summary = new TimesheetVarianceSummary(
                BigDecimal.valueOf(30.0), BigDecimal.valueOf(38.0), BigDecimal.valueOf(8.0),
                1, 1, 1, 0, 0, 0
        );

        TimesheetVarianceResult mockResult = new TimesheetVarianceResult(
                10L, "Phòng Kỹ thuật", 2026, 35, 2026, 35,
                List.of(item), summary, true, "Lấy báo cáo thành công", LocalDateTime.now()
        );

        when(getTimesheetVarianceUseCase.execute(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/reports/timesheet-variance")
                        .param("orgUnitId", "10")
                        .param("fromYear", "2026")
                        .param("fromWeek", "35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data.items[0].allocatedHours").value(30.0))
                .andExpect(jsonPath("$.data.items[0].actualApprovedHours").value(38.0))
                .andExpect(jsonPath("$.data.items[0].varianceHours").value(8.0))
                .andExpect(jsonPath("$.data.items[0].varianceStatus").value("POSITIVE_VARIANCE"));
    }

    @Test
    @DisplayName("TC-03: API Từ chối người dùng không có quyền (HTTP 403 Forbidden)")
    void getTimesheetVarianceReport_Forbidden() throws Exception {
        when(getTimesheetVarianceUseCase.execute(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.TIMESHEET_VARIANCE_READ));

        mockMvc.perform(get("/api/v1/reports/timesheet-variance"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("TC-04: Đảm bảo Controller được bảo vệ bởi @PreAuthorize('hasAuthority(\\'TIMESHEET_VARIANCE_READ\\')')")
    void getTimesheetVarianceReport_SecurityAnnotationPresent() throws NoSuchMethodException {
        var method = TimesheetVarianceReportController.class.getMethod(
                "getTimesheetVarianceReport",
                Long.class, Long.class, Long.class, Integer.class, Integer.class, Integer.class, Integer.class
        );
        var preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuthorize, "Method getTimesheetVarianceReport must have @PreAuthorize");
        org.junit.jupiter.api.Assertions.assertEquals("hasAuthority('TIMESHEET_VARIANCE_READ')", preAuthorize.value());
    }
}
