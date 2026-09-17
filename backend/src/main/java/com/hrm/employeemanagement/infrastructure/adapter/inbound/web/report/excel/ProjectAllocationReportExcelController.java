package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.excel;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelQuery;
import com.hrm.employeemanagement.application.dto.report.excel.ExportReportExcelResult;
import com.hrm.employeemanagement.application.port.inbound.report.excel.ExportProjectAllocationReportExcelUseCase;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.availability.InvalidWeekNumberException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.report.excel.exception.NoReportDataToExportException;
import com.hrm.employeemanagement.domain.report.excel.exception.ReportExportAuditException;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.common.ErrorResponse;

/**
 * REST Controller cung cấp API Xuất báo cáo phân bổ dự án ra file Excel (NCL-10-CN-003).
 * Dành riêng cho Quản lý dự án (VT-02) và Ban Giám Đốc (VT-01).
 */
@RestController
@RequestMapping("/api/v1/reports/export/excel")
@Validated
public class ProjectAllocationReportExcelController {

    private final ExportProjectAllocationReportExcelUseCase exportProjectAllocationReportExcelUseCase;

    public ProjectAllocationReportExcelController(ExportProjectAllocationReportExcelUseCase exportProjectAllocationReportExcelUseCase) {
        this.exportProjectAllocationReportExcelUseCase = exportProjectAllocationReportExcelUseCase;
    }

    /**
     * API Xuất báo cáo phân bổ dự án theo tuần ra file Excel (.xlsx).
     *
     * @param projectId ID dự án cần xuất (bắt buộc)
     * @param fromYear Năm bắt đầu (tùy chọn)
     * @param fromWeek Số tuần bắt đầu (tùy chọn)
     * @param toYear Năm kết thúc (tùy chọn)
     * @param toWeek Số tuần kết thúc (tùy chọn)
     * @return File Excel dưới dạng nhị phân đính kèm
     */
    @GetMapping("/project-allocation")
    @PreAuthorize("hasAuthority('RESOURCE_ALLOCATION_READ')")
    public ResponseEntity<byte[]> exportProjectAllocationReport(
            @RequestParam Long projectId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek,
            @RequestParam(required = false, defaultValue = "false") Boolean all
    ) {
        ExportReportExcelQuery query = new ExportReportExcelQuery(projectId, fromYear, fromWeek, toYear, toWeek, all);
        ExportReportExcelResult result = exportProjectAllocationReportExcelUseCase.export(query);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    @ExceptionHandler(NoReportDataToExportException.class)
    public ResponseEntity<ErrorResponse> handleNoReportData(NoReportDataToExportException ex) {
        ErrorResponse error = ErrorResponse.of(
                "REPORT_NO_DATA",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(PermissionDeniedException ex) {
        ErrorResponse error = ErrorResponse.of(
                "FORBIDDEN",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(ProjectNotFoundException ex) {
        ErrorResponse error = ErrorResponse.of(
                "PROJECT_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ReportExportAuditException.class)
    public ResponseEntity<ErrorResponse> handleAuditException(ReportExportAuditException ex) {
        ErrorResponse error = ErrorResponse.of(
                "AUDIT_LOG_FAILED",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler({InvalidWeekNumberException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(Exception ex) {
        ErrorResponse error = ErrorResponse.of(
                "INVALID_INPUT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
