package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.workload;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.workload.GetUpcomingWorkloadQuery;
import com.hrm.employeemanagement.application.dto.workload.UpcomingWorkloadResult;
import com.hrm.employeemanagement.application.port.inbound.workload.GetUpcomingWorkloadUseCase;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/workload")
public class UpcomingWorkloadController {

    private final GetUpcomingWorkloadUseCase getUpcomingWorkloadUseCase;

    public UpcomingWorkloadController(GetUpcomingWorkloadUseCase getUpcomingWorkloadUseCase) {
        this.getUpcomingWorkloadUseCase = Objects.requireNonNull(getUpcomingWorkloadUseCase, "getUpcomingWorkloadUseCase must not be null");
    }

    /**
     * NCL-13-CN-004: Xem khối lượng công việc sắp tới của chính tôi (8 tuần tới).
     * Dành cho nhân viên chuyên môn (VT-04) và người dùng đã xác thực.
     */
    @GetMapping("/my-upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UpcomingWorkloadResult> getMyUpcomingWorkload(
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false, defaultValue = "8") Integer durationWeeks
    ) {
        UpcomingWorkloadResult result = getUpcomingWorkloadUseCase.getMyUpcomingWorkload(fromYear, fromWeek, durationWeeks);
        return ResponseEntity.ok(result);
    }

    /**
     * NCL-13-CN-004: Xem khối lượng công việc sắp tới của một nhân sự cụ thể.
     * Kiểm tra chặt chẽ DataScope (COMPANY / ORGANIZATION_BRANCH / SELF).
     */
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UpcomingWorkloadResult> getEmployeeUpcomingWorkload(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer fromYear,
            @RequestParam(required = false) Integer fromWeek,
            @RequestParam(required = false, defaultValue = "8") Integer durationWeeks
    ) {
        GetUpcomingWorkloadQuery query = new GetUpcomingWorkloadQuery(employeeId, fromYear, fromWeek, durationWeeks);
        UpcomingWorkloadResult result = getUpcomingWorkloadUseCase.getUpcomingWorkload(query);
        return ResponseEntity.ok(result);
    }
}
