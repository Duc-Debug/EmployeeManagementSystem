package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.timesheetvariance;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceQuery;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceResult;
import com.hrm.employeemanagement.application.port.inbound.report.timesheetvariance.GetTimesheetVarianceUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * REST Controller cung cấp API Báo cáo đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
 */
@RestController
@RequestMapping("/api/v1/reports/timesheet-variance")
@Validated
public class TimesheetVarianceReportController {

    private final GetTimesheetVarianceUseCase getTimesheetVarianceUseCase;

    public TimesheetVarianceReportController(GetTimesheetVarianceUseCase getTimesheetVarianceUseCase) {
        this.getTimesheetVarianceUseCase = getTimesheetVarianceUseCase;
    }

    /**
     * API Đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
     * Dành cho Ban Giám đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('TIMESHEET_VARIANCE_READ')")
    public ResponseEntity<ApiResponse<TimesheetVarianceResult>> getTimesheetVarianceReport(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek
    ) {
        TimesheetVarianceQuery query = new TimesheetVarianceQuery(
                orgUnitId,
                employeeId,
                projectId,
                fromYear,
                fromWeek,
                toYear,
                toWeek
        );
        TimesheetVarianceResult result = getTimesheetVarianceUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success(result.message(), result));
    }
}
