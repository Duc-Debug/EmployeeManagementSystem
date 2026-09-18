package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.billablerate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateExport;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateQuery;
import com.hrm.employeemanagement.application.dto.report.billablerate.BillableRateResult;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.ExportBillableRateReportUseCase;
import com.hrm.employeemanagement.application.port.inbound.report.billablerate.GetBillableRateReportUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * REST Controller cung cấp API Báo cáo tỷ lệ giờ tính phí (NCL-10-CN-002).
 * Dành cho Ban Giám đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06).
 */
@RestController
@RequestMapping("/api/v1/reports/billable-rate")
@Validated
public class BillableRateReportController {

    private final GetBillableRateReportUseCase getBillableRateReportUseCase;
    private final ExportBillableRateReportUseCase exportBillableRateReportUseCase;

    public BillableRateReportController(
            GetBillableRateReportUseCase getBillableRateReportUseCase,
            ExportBillableRateReportUseCase exportBillableRateReportUseCase
    ) {
        this.getBillableRateReportUseCase = getBillableRateReportUseCase;
        this.exportBillableRateReportUseCase = exportBillableRateReportUseCase;
    }

    /**
     * API Lấy báo cáo tỷ lệ giờ tính phí (NCL-10-CN-002).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('BILLABLE_HOURS_REPORT_READ')")
    public ResponseEntity<ApiResponse<BillableRateResult>> getBillableRateReport(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek
    ) {
        BillableRateQuery query = new BillableRateQuery(
                orgUnitId,
                employeeId,
                fromYear,
                fromWeek,
                toYear,
                toWeek
        );
        BillableRateResult result = getBillableRateReportUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result.message(), result));
    }

    /**
     * API Xuất CSV báo cáo tỷ lệ giờ tính phí.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('BILLABLE_HOURS_REPORT_READ')")
    public ResponseEntity<byte[]> exportBillableRateReport(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek
    ) {
        BillableRateQuery query = new BillableRateQuery(
                orgUnitId,
                employeeId,
                fromYear,
                fromWeek,
                toYear,
                toWeek
        );
        BillableRateExport export = exportBillableRateReportUseCase.export(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.filename() + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(export.content());
    }
}
