package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.projectallocation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportExport;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportQuery;
import com.hrm.employeemanagement.application.dto.report.projectallocation.ProjectAllocationReportResult;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.ExportProjectAllocationReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.projectallocation.GetProjectAllocationReportUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * REST Controller cung cấp API Báo cáo phân bổ theo dự án (NCL-10-CN-006).
 * Dành cho Quản lý dự án (VT-02), Ban Giám đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06).
 */
@RestController
@RequestMapping("/api/v1/reports/project-allocation")
@Validated
public class ProjectAllocationReportController {

    private final GetProjectAllocationReportUseCase getProjectAllocationReportUseCase;
    private final ExportProjectAllocationReportUseCase exportProjectAllocationReportUseCase;

    public ProjectAllocationReportController(
            GetProjectAllocationReportUseCase getProjectAllocationReportUseCase,
            ExportProjectAllocationReportUseCase exportProjectAllocationReportUseCase
    ) {
        this.getProjectAllocationReportUseCase = getProjectAllocationReportUseCase;
        this.exportProjectAllocationReportUseCase = exportProjectAllocationReportUseCase;
    }

    /**
     * API Lấy báo cáo phân bổ nguồn lực theo dự án theo tuần và vai trò.
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectAllocationReportResult>> getProjectAllocationReport(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek
    ) {
        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, fromYear, fromWeek, toYear, toWeek);
        ProjectAllocationReportResult result = getProjectAllocationReportUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo phân bổ theo dự án thành công", result));
    }

    /**
     * API Xuất CSV báo cáo phân bổ nguồn lực theo dự án.
     */
    @GetMapping("/{projectId}/export")
    public ResponseEntity<byte[]> exportProjectAllocationReport(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek
    ) {
        ProjectAllocationReportQuery query = new ProjectAllocationReportQuery(projectId, fromYear, fromWeek, toYear, toWeek);
        ProjectAllocationReportExport export = exportProjectAllocationReportUseCase.export(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(export.content());
    }
}
