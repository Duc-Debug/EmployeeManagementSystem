package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.NotifyScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ScanScheduleConflictsUseCase;
import com.hrm.employeemanagement.domain.conflict.ConflictType;
import com.hrm.employeemanagement.domain.conflict.ScheduleConflictStatus;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/schedule-conflicts")
public class ScheduleConflictWarningController {

    private final GetScheduleConflictsUseCase getUseCase;
    private final ScanScheduleConflictsUseCase scanUseCase;
    private final NotifyScheduleConflictUseCase notifyUseCase;
    private final ResolveScheduleConflictUseCase resolveUseCase;

    public ScheduleConflictWarningController(
            GetScheduleConflictsUseCase getUseCase,
            ScanScheduleConflictsUseCase scanUseCase,
            NotifyScheduleConflictUseCase notifyUseCase,
            ResolveScheduleConflictUseCase resolveUseCase
    ) {
        this.getUseCase = getUseCase;
        this.scanUseCase = scanUseCase;
        this.notifyUseCase = notifyUseCase;
        this.resolveUseCase = resolveUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleConflictResult>>> getScheduleConflicts(
            @RequestParam(required = false) Integer yearNumber,
            @RequestParam(required = false) Integer startWeek,
            @RequestParam(required = false) Integer endWeek,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) ConflictType conflictType,
            @RequestParam(required = false) ScheduleConflictStatus status
    ) {
        ScheduleConflictQuery query = new ScheduleConflictQuery(
                yearNumber, startWeek, endWeek, employeeId, projectId, conflictType, status
        );

        List<ScheduleConflictResult> results = getUseCase.getScheduleConflicts(query);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cảnh báo xung đột lịch thành công", results));
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<List<ScheduleConflictResult>>> scanScheduleConflicts(
            @RequestParam(required = false) Integer yearNumber,
            @RequestParam(required = false) Integer startWeek,
            @RequestParam(required = false) Integer endWeek
    ) {
        List<ScheduleConflictResult> results = scanUseCase.scanScheduleConflicts(yearNumber, startWeek, endWeek);
        return ResponseEntity.ok(ApiResponse.success("Rà soát cảnh báo xung đột lịch thành công", results));
    }

    @PostMapping("/{id}/notify")
    public ResponseEntity<ApiResponse<ScheduleConflictResult>> notifyScheduleConflict(@PathVariable Long id) {
        ScheduleConflictResult result = notifyUseCase.notifyScheduleConflict(id);
        return ResponseEntity.ok(ApiResponse.success("Gửi thông báo cảnh báo xung đột lịch thành công", result));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ScheduleConflictResult>> resolveScheduleConflict(@PathVariable Long id) {
        ScheduleConflictResult result = resolveUseCase.resolveScheduleConflict(id);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đã xử lý xung đột lịch thành công", result));
    }
}
