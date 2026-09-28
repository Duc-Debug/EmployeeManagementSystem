package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.projectallocation;

import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportExport;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportResult;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.ExportProjectAllocationReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.GetProjectAllocationReportUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAllocationReportController Tests (NCL-10-CN-006)")
class ProjectAllocationReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetProjectAllocationReportUseCase getProjectAllocationReportUseCase;

    @Mock
    private ExportProjectAllocationReportUseCase exportProjectAllocationReportUseCase;

    @InjectMocks
    private ProjectAllocationReportController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API - Lấy báo cáo phân bổ theo dự án thành công (HTTP 200 OK)")
    void testGetProjectAllocationReport_Success() throws Exception {
        ProjectAllocationReportResult mockResult = new ProjectAllocationReportResult(
                1L,
                "PRJ-001",
                "Hệ thống HRM Core",
                "ACTIVE",
                10L,
                "Trung tâm phần mềm",
                20L,
                "Nguyễn Văn PM",
                "2026-09-14",
                "2026-09-20",
                2026, 38,
                2026, 38,
                BigDecimal.valueOf(160.0),
                BigDecimal.valueOf(40.0),
                BigDecimal.valueOf(30.0),
                BigDecimal.valueOf(10.0),
                BigDecimal.ZERO,
                BigDecimal.valueOf(75.0),
                1,
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now()
        );

        when(getProjectAllocationReportUseCase.execute(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/reports/project-allocation/1")
                        .param("fromYear", "2026")
                        .param("fromWeek", "38")
                        .param("toYear", "2026")
                        .param("toWeek", "38"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(1))
                .andExpect(jsonPath("$.data.projectCode").value("PRJ-001"))
                .andExpect(jsonPath("$.data.totalDemandHours").value(40.0))
                .andExpect(jsonPath("$.data.totalAllocatedHours").value(30.0))
                .andExpect(jsonPath("$.data.totalShortfallHours").value(10.0));
    }

    @Test
    @DisplayName("API - Bị từ chối khi không có quyền hoặc ngoài scope (HTTP 403 Forbidden)")
    void testGetProjectAllocationReport_Forbidden() throws Exception {
        when(getProjectAllocationReportUseCase.execute(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_ALLOCATION_REPORT_READ));

        mockMvc.perform(get("/api/v1/reports/project-allocation/999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("API - Xuất CSV báo cáo phân bổ theo dự án thành công (HTTP 200 OK)")
    void testExportProjectAllocationReport_Success() throws Exception {
        ProjectAllocationReportExport mockExport = new ProjectAllocationReportExport(
                "Bao_cao_phan_bo_PRJ-001.csv",
                "Mã dự án,PRJ-001\n".getBytes()
        );

        when(exportProjectAllocationReportUseCase.export(any())).thenReturn(mockExport);

        mockMvc.perform(get("/api/v1/reports/project-allocation/1/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"Bao_cao_phan_bo_PRJ-001.csv\""));
    }
}
