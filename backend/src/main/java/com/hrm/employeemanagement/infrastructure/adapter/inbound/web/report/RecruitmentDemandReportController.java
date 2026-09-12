package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.report;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportQuery;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandReportResult;
import com.hrm.employeemanagement.application.port.inbound.report.GetRecruitmentDemandReportUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/reports/recruitment-demand")
@Validated
public class RecruitmentDemandReportController {

    private final GetRecruitmentDemandReportUseCase getRecruitmentDemandReportUseCase;

    public RecruitmentDemandReportController(GetRecruitmentDemandReportUseCase getRecruitmentDemandReportUseCase) {
        this.getRecruitmentDemandReportUseCase = getRecruitmentDemandReportUseCase;
    }

    /**
     * API Báo cáo nhu cầu tuyển dụng theo kỹ năng (NCL-10-CN-005).
     * Dành riêng cho Ban Giám đốc (VT-01), HR/Quản lý nguồn lực (VT-03), Quản trị viên (VT-06).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('RECRUITMENT_DEMAND_REPORT_READ')")
    public ResponseEntity<ApiResponse<RecruitmentDemandReportResult>> getRecruitmentDemandReport(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Integer toWeek,
            @RequestParam(required = false) Long orgUnitId
    ) {
        RecruitmentDemandReportQuery query = new RecruitmentDemandReportQuery(fromYear, fromWeek, toYear, toWeek, orgUnitId);
        RecruitmentDemandReportResult result = getRecruitmentDemandReportUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo nhu cầu tuyển dụng thành công", result));
    }
}
