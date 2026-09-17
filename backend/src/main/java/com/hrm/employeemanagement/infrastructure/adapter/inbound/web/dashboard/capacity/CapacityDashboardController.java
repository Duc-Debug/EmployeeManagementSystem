package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.dashboard.capacity;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardQuery;
import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardResult;
import com.hrm.employeemanagement.application.port.inbound.dashboard.capacity.GetCapacityDashboardUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

/**
 * REST Controller cung cấp API Bảng điều khiển năng lực (NCL-10-CN-001).
 * Cung cấp 5 chỉ số chính và bức tranh năng lực toàn diện theo kỳ.
 */
@RestController
@RequestMapping("/api/v1/capacity-dashboard")
@Validated
public class CapacityDashboardController {

    private final GetCapacityDashboardUseCase getCapacityDashboardUseCase;

    public CapacityDashboardController(GetCapacityDashboardUseCase getCapacityDashboardUseCase) {
        this.getCapacityDashboardUseCase = getCapacityDashboardUseCase;
    }

    /**
     * API Lấy dữ liệu Bảng điều khiển năng lực.
     * Mặc định hiển thị kỳ 8 tuần tới nếu không chỉ định fromYear/fromWeek/durationWeeks.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CapacityDashboardResult>> getCapacityDashboard(
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false, defaultValue = "8") Integer durationWeeks
    ) {
        CapacityDashboardQuery query = new CapacityDashboardQuery(orgUnitId, fromYear, fromWeek, durationWeeks);
        CapacityDashboardResult result = getCapacityDashboardUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu bảng điều khiển năng lực thành công", result));
    }
}
