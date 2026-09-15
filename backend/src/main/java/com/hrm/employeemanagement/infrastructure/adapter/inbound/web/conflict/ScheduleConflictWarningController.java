package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.conflict;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.conflict.AssignScheduleConflictHandlerCommand;
import com.hrm.employeemanagement.application.dto.conflict.ResolveScheduleConflictWithNoteCommand;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictQuery;
import com.hrm.employeemanagement.application.dto.conflict.ScheduleConflictResult;
import com.hrm.employeemanagement.application.port.inbound.conflict.AssignScheduleConflictHandlerUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.GetScheduleConflictsUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.NotifyScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictUseCase;
import com.hrm.employeemanagement.application.port.inbound.conflict.ResolveScheduleConflictWithNoteUseCase;
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
    private final ResolveScheduleConflictWithNoteUseCase resolveWithNoteUseCase;
    private final AssignScheduleConflictHandlerUseCase assignHandlerUseCase;

    public ScheduleConflictWarningController(
            GetScheduleConflictsUseCase getUseCase,
            ScanScheduleConflictsUseCase scanUseCase,
            NotifyScheduleConflictUseCase notifyUseCase,
            ResolveScheduleConflictUseCase resolveUseCase,
            ResolveScheduleConflictWithNoteUseCase resolveWithNoteUseCase,
            AssignScheduleConflictHandlerUseCase assignHandlerUseCase
    ) {
        this.getUseCase = getUseCase;
        this.scanUseCase = scanUseCase;
        this.notifyUseCase = notifyUseCase;
        this.resolveUseCase = resolveUseCase;
        this.resolveWithNoteUseCase = resolveWithNoteUseCase;
        this.assignHandlerUseCase = assignHandlerUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('RESOURCE_SCHEDULE_CONFLICT_READ', 'RESOURCE_SCHEDULE_CONFLICT_NOTIFY', 'RESOURCE_CONFLICT_HANDLE')")
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
    @PreAuthorize("hasAnyAuthority('RESOURCE_SCHEDULE_CONFLICT_NOTIFY', 'RESOURCE_CONFLICT_HANDLE')")
    public ResponseEntity<ApiResponse<List<ScheduleConflictResult>>> scanScheduleConflicts(
            @RequestParam(required = false) Integer yearNumber,
            @RequestParam(required = false) Integer startWeek,
            @RequestParam(required = false) Integer endWeek
    ) {
        List<ScheduleConflictResult> results = scanUseCase.scanScheduleConflicts(yearNumber, startWeek, endWeek);
        return ResponseEntity.ok(ApiResponse.success("Rà soát cảnh báo xung đột lịch thành công", results));
    }

    @PostMapping("/{id}/notify")
    @PreAuthorize("hasAnyAuthority('RESOURCE_SCHEDULE_CONFLICT_NOTIFY', 'RESOURCE_CONFLICT_HANDLE')")
    public ResponseEntity<ApiResponse<ScheduleConflictResult>> notifyScheduleConflict(@PathVariable Long id) {
        ScheduleConflictResult result = notifyUseCase.notifyScheduleConflict(id);
        return ResponseEntity.ok(ApiResponse.success("Gửi thông báo cảnh báo xung đột lịch thành công", result));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('RESOURCE_SCHEDULE_CONFLICT_NOTIFY', 'RESOURCE_CONFLICT_HANDLE')")
    public ResponseEntity<ApiResponse<ScheduleConflictResult>> resolveScheduleConflict(@PathVariable Long id) {
        ScheduleConflictResult result = resolveUseCase.resolveScheduleConflict(id);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đã xử lý xung đột lịch thành công", result));
    }

    @PostMapping("/{id}/resolve-with-note")
    @PreAuthorize("hasAnyAuthority('RESOURCE_CONFLICT_HANDLE', 'RESOURCE_SCHEDULE_CONFLICT_NOTIFY')")
    public ResponseEntity<ApiResponse<ScheduleConflictResult>> resolveScheduleConflictWithNote(
            @PathVariable Long id,
            @RequestBody ResolveScheduleConflictWithNoteCommand command
    ) {
        ResolveScheduleConflictWithNoteCommand targetCommand = new ResolveScheduleConflictWithNoteCommand(
                id,
                command.assignedHandlerId(),
                command.resolutionNote()
        );
        ScheduleConflictResult result = resolveWithNoteUseCase.resolveScheduleConflictWithNote(targetCommand);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu đã xử lý xung đột lịch kèm cách xử lý thành công", result));
    }

    @PostMapping("/{id}/assign-handler")
    @PreAuthorize("hasAnyAuthority('RESOURCE_CONFLICT_HANDLE', 'RESOURCE_SCHEDULE_CONFLICT_NOTIFY')")
    public ResponseEntity<ApiResponse<ScheduleConflictResult>> assignScheduleConflictHandler(
            @PathVariable Long id,
            @RequestBody AssignScheduleConflictHandlerCommand command
    ) {
        AssignScheduleConflictHandlerCommand targetCommand = new AssignScheduleConflictHandlerCommand(
                id,
                command.assignedHandlerId()
        );
        ScheduleConflictResult result = assignHandlerUseCase.assignScheduleConflictHandler(targetCommand);
        return ResponseEntity.ok(ApiResponse.success("Gán người xử lý xung đột lịch thành công", result));
    }
}
