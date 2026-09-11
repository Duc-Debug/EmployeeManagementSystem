package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;
import com.hrm.employeemanagement.application.dto.leave.GetDepartmentMonthlyLeaveCalendarQuery;
import com.hrm.employeemanagement.application.port.inbound.leave.GetDepartmentMonthlyLeaveCalendarUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.leave.dto.DepartmentMonthlyLeaveCalendarResponse;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Objects;

/**
 * REST Controller cho chức năng xem Lịch nghỉ của bộ phận theo tháng (NCL-05-CN-006).
 * Phân quyền: Dành cho Quản lý nguồn lực (VT-03), Ban giám đốc (VT-01), Quản trị viên (VT-06).
 */
@RestController
@RequestMapping("/api/v1/leave-requests/department-calendar")
@Validated
public class DepartmentLeaveCalendarController {

    private final GetDepartmentMonthlyLeaveCalendarUseCase getDepartmentMonthlyLeaveCalendarUseCase;

    public DepartmentLeaveCalendarController(GetDepartmentMonthlyLeaveCalendarUseCase getDepartmentMonthlyLeaveCalendarUseCase) {
        this.getDepartmentMonthlyLeaveCalendarUseCase = Objects.requireNonNull(getDepartmentMonthlyLeaveCalendarUseCase, "getDepartmentMonthlyLeaveCalendarUseCase must not be null");
    }

    /**
     * Tra cứu lịch nghỉ bộ phận theo query param:
     * GET /api/v1/leave-requests/department-calendar?orgUnitId={id}&year={y}&month={m}&warningThreshold={rate}
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('VT-03', 'VT-01', 'VT-06', 'ROLE_VT-03', 'ROLE_VT-01', 'ROLE_VT-06', 'DEPARTMENT_LEAVE_READ')")
    public ResponseEntity<ApiResponse<DepartmentMonthlyLeaveCalendarResponse>> getDepartmentLeaveCalendar(
            @RequestParam @NotNull(message = "ID bộ phận (orgUnitId) không được để trống")
            @Positive(message = "ID bộ phận phải là số dương") Long orgUnitId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Min(value = 1, message = "Tháng phải từ 1 đến 12") @Max(value = 12, message = "Tháng phải từ 1 đến 12") Integer month,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "Ngưỡng cảnh báo phải lớn hơn 0") @DecimalMax(value = "1.0", message = "Ngưỡng cảnh báo tối đa là 1.0 (100%)") Double warningThreshold
    ) {
        LocalDate now = LocalDate.now();
        int targetYear = (year != null && year > 1970) ? year : now.getYear();
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();

        GetDepartmentMonthlyLeaveCalendarQuery query = new GetDepartmentMonthlyLeaveCalendarQuery(
                orgUnitId,
                targetYear,
                targetMonth,
                warningThreshold
        );

        DepartmentMonthlyLeaveCalendarResult result = getDepartmentMonthlyLeaveCalendarUseCase.execute(query);
        DepartmentMonthlyLeaveCalendarResponse response = DepartmentMonthlyLeaveCalendarResponse.from(result);

        return ResponseEntity.ok(ApiResponse.success("Lấy lịch nghỉ của bộ phận theo tháng thành công", response));
    }

    /**
     * Tra cứu lịch nghỉ bộ phận qua path variable:
     * GET /api/v1/leave-requests/department-calendar/{orgUnitId}?year={y}&month={m}&warningThreshold={rate}
     */
    @GetMapping("/{orgUnitId}")
    @PreAuthorize("hasAnyAuthority('VT-03', 'VT-01', 'VT-06', 'ROLE_VT-03', 'ROLE_VT-01', 'ROLE_VT-06', 'DEPARTMENT_LEAVE_READ')")
    public ResponseEntity<ApiResponse<DepartmentMonthlyLeaveCalendarResponse>> getDepartmentLeaveCalendarByPath(
            @PathVariable @NotNull(message = "ID bộ phận không được để trống")
            @Positive(message = "ID bộ phận phải là số dương") Long orgUnitId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @Min(value = 1, message = "Tháng phải từ 1 đến 12") @Max(value = 12, message = "Tháng phải từ 1 đến 12") Integer month,
            @RequestParam(required = false) @DecimalMin(value = "0.01", message = "Ngưỡng cảnh báo phải lớn hơn 0") @DecimalMax(value = "1.0", message = "Ngưỡng cảnh báo tối đa là 1.0 (100%)") Double warningThreshold
    ) {
        return getDepartmentLeaveCalendar(orgUnitId, year, month, warningThreshold);
    }
}
