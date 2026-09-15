package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report.capacityforecast;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastQuery;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult;
import com.hrm.employeemanagement.application.port.inbound.report.capacityforecast.GetCapacityForecastUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * REST Controller cung cấp API Báo cáo dự báo năng lực các tuần tới (NCL-10-CN-004).
 */
@RestController
@RequestMapping("/api/v1/reports/capacity-forecast")
@Validated
public class CapacityForecastReportController {

    private final GetCapacityForecastUseCase getCapacityForecastUseCase;

    public CapacityForecastReportController(GetCapacityForecastUseCase getCapacityForecastUseCase) {
        this.getCapacityForecastUseCase = getCapacityForecastUseCase;
    }

    /**
     * API Báo cáo dự báo năng lực các tuần tới (NCL-10-CN-004).
     * Dành cho Ban Giám đốc (VT-01) và Quản lý nguồn lực (VT-03).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CapacityForecastResult>> getCapacityForecastReport(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false, defaultValue = "12") Integer durationWeeks
    ) {
        CapacityForecastQuery query = new CapacityForecastQuery(orgUnitId, fromYear, fromWeek, durationWeeks);
        CapacityForecastResult result = getCapacityForecastUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo dự báo năng lực thành công", result));
    }
}

