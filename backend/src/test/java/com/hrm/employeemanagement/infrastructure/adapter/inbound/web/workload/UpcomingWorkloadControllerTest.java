package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workload;

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

import com.hrm.employeemanagement.application.dto.workload.ProjectWorkloadAllocationResult;
import com.hrm.employeemanagement.application.dto.workload.UpcomingWorkloadResult;
import com.hrm.employeemanagement.application.dto.workload.WeeklyWorkloadItemResult;
import com.hrm.employeemanagement.application.dto.workload.WorkloadSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.workload.GetUpcomingWorkloadUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpcomingWorkloadController Web Adapter Tests (NCL-13-CN-004)")
class UpcomingWorkloadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetUpcomingWorkloadUseCase getUpcomingWorkloadUseCase;

    @InjectMocks
    private UpcomingWorkloadController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API - GET /api/v1/workload/my-upcoming thành công (HTTP 200 OK)")
    void testGetMyUpcomingWorkload_Success() throws Exception {
        ProjectWorkloadAllocationResult projAlloc = new ProjectWorkloadAllocationResult(
                50L, "Dự án ERP", "PRJ-050", 1L, "Backend Dev", BigDecimal.valueOf(30.0), BigDecimal.valueOf(75.0)
        );

        WeeklyWorkloadItemResult week1 = new WeeklyWorkloadItemResult(
                2026, 40, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 4), "T40/2026",
                40, 0, BigDecimal.ZERO, BigDecimal.valueOf(40.0), BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(75.0), "NORMAL", BigDecimal.ZERO, List.of(projAlloc)
        );

        WorkloadSummaryResult summary = new WorkloadSummaryResult(
                BigDecimal.valueOf(320.0), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(320.0), BigDecimal.valueOf(240.0), BigDecimal.valueOf(75.0),
                0, 8, 0, BigDecimal.valueOf(75.0), "T40/2026"
        );

        UpcomingWorkloadResult mockResult = new UpcomingWorkloadResult(
                10L, "EMP010", "Nguyen Van Dev", 1L, "Phòng PTPM",
                2026, 40, 8, BigDecimal.valueOf(100.0), BigDecimal.valueOf(70.0),
                List.of(week1), summary, LocalDateTime.now()
        );

        when(getUpcomingWorkloadUseCase.getMyUpcomingWorkload(any(), any(), any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/workload/my-upcoming")
                        .param("fromYear", "2026")
                        .param("fromWeek", "40")
                        .param("durationWeeks", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(10))
                .andExpect(jsonPath("$.employeeName").value("Nguyen Van Dev"))
                .andExpect(jsonPath("$.weeklyWorkloads[0].weekNumber").value(40))
                .andExpect(jsonPath("$.weeklyWorkloads[0].status").value("NORMAL"))
                .andExpect(jsonPath("$.weeklyWorkloads[0].projectAllocations[0].projectName").value("Dự án ERP"));
    }

    @Test
    @DisplayName("API - GET /api/v1/workload/employee/{employeeId} bị từ chối quyền trả về HTTP 403 Forbidden")
    void testGetEmployeeUpcomingWorkload_PermissionDenied_Returns403() throws Exception {
        when(getUpcomingWorkloadUseCase.getUpcomingWorkload(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.EMPLOYEE_READ));

        mockMvc.perform(get("/api/v1/workload/employee/99")
                        .param("fromYear", "2026")
                        .param("fromWeek", "40"))
                .andExpect(status().isForbidden());
    }
}
