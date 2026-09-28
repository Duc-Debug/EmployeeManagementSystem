package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.excel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelResult;
import com.hrm.employeemanagement.application.port.inbound.report.excel.ExportProjectAllocationReportExcelUseCase;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.report.excel.exception.NoReportDataToExportException;
import com.hrm.employeemanagement.domain.report.excel.exception.ReportExportAuditException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectAllocationReportExcelController Web Adapter Tests")
class ProjectAllocationReportExcelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExportProjectAllocationReportExcelUseCase exportUseCase;

    @InjectMocks
    private ProjectAllocationReportExcelController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-01: API - Xuất báo cáo phân bổ dự án ra file Excel thành công (200 OK)")
    void testExportReport_Success() throws Exception {
        byte[] fakeContent = "excel_binary_data".getBytes();
        ExportReportExcelResult mockResult = new ExportReportExcelResult(
                "Bao_Cao_Phan_Bo_PRJ_001_20260917.xlsx",
                fakeContent,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        when(exportUseCase.export(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/reports/export/excel/project-allocation")
                        .param("projectId", "1")
                        .param("fromYear", "2026")
                        .param("fromWeek", "1")
                        .param("toYear", "2026")
                        .param("toWeek", "4"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bao_Cao_Phan_Bo_PRJ_001_20260917.xlsx\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(fakeContent));
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-02: API - Kỳ không có dữ liệu trả về 400 Bad Request kèm thông báo")
    void testExportReport_NoData_Returns400() throws Exception {
        when(exportUseCase.export(any()))
                .thenThrow(new NoReportDataToExportException("Không có dữ liệu để xuất trong kỳ đã chọn"));

        mockMvc.perform(get("/api/v1/reports/export/excel/project-allocation")
                        .param("projectId", "1")
                        .param("fromYear", "2026")
                        .param("fromWeek", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REPORT_NO_DATA"))
                .andExpect(jsonPath("$.message").value("Không có dữ liệu để xuất trong kỳ đã chọn"));
    }

    @Test
    @DisplayName("NCL-10-CN-003-TC-03: API - Không có quyền truy cập trả về 403 Forbidden")
    void testExportReport_Forbidden_Returns403() throws Exception {
        when(exportUseCase.export(any()))
                .thenThrow(new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ));

        mockMvc.perform(get("/api/v1/reports/export/excel/project-allocation")
                        .param("projectId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("API - Dự án không tồn tại trả về 404 Not Found")
    void testExportReport_ProjectNotFound_Returns404() throws Exception {
        when(exportUseCase.export(any()))
                .thenThrow(new ProjectNotFoundException("Không tìm thấy dự án với ID: 999"));

        mockMvc.perform(get("/api/v1/reports/export/excel/project-allocation")
                        .param("projectId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("QTN-02: API - Lỗi ghi nhật ký kiểm toán trả về 500 Internal Server Error và chặn xuất file")
    void testExportReport_AuditFailed_Returns500() throws Exception {
        when(exportUseCase.export(any()))
                .thenThrow(new ReportExportAuditException("Không thể ghi nhận nhật ký kiểm toán theo quy tắc QTN-02"));

        mockMvc.perform(get("/api/v1/reports/export/excel/project-allocation")
                        .param("projectId", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("AUDIT_LOG_FAILED"));
    }

    @Test
    @DisplayName("BLOCKER 2: API - Số tuần không hợp lệ (InvalidWeekNumberException) trả về 400 Bad Request thay vì 500")
    void testExportReport_InvalidWeekNumber_Returns400() throws Exception {
        when(exportUseCase.export(any()))
                .thenThrow(new com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException("Số tuần không hợp lệ: 55"));

        mockMvc.perform(get("/api/v1/reports/export/excel/project-allocation")
                        .param("projectId", "1")
                        .param("fromYear", "2026")
                        .param("fromWeek", "55"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
