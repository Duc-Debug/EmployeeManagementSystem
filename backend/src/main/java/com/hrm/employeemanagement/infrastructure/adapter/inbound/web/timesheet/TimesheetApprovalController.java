package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogResult;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.AdjustApprovedWorkLogUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ApproveTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetPendingApprovalsUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.timesheet.dto.AdjustApprovedWorkLogRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/work-logs/approvals")
public class TimesheetApprovalController {

    private final ApproveTimesheetUseCase approveTimesheetUseCase;
    private final GetPendingApprovalsUseCase getPendingApprovalsUseCase;
    private final AdjustApprovedWorkLogUseCase adjustApprovedWorkLogUseCase;

    public TimesheetApprovalController(
            ApproveTimesheetUseCase approveTimesheetUseCase,
            GetPendingApprovalsUseCase getPendingApprovalsUseCase,
            AdjustApprovedWorkLogUseCase adjustApprovedWorkLogUseCase) {
        this.approveTimesheetUseCase = approveTimesheetUseCase;
        this.getPendingApprovalsUseCase = getPendingApprovalsUseCase;
        this.adjustApprovedWorkLogUseCase = adjustApprovedWorkLogUseCase;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<WorkLogResult>> getPendingApprovals() {
        return ResponseEntity.ok(getPendingApprovalsUseCase.getPendingApprovals());
    }

    @PutMapping("/entries/{entryId}/approve")
    public ResponseEntity<ApproveTimesheetUseCase.ApprovalResult> approveEntry(
            @PathVariable Long entryId,
            @RequestBody Map<String, Object> request) {
        Long version = request.containsKey("version") ? Long.valueOf(request.get("version").toString()) : null;
        return ResponseEntity.ok(approveTimesheetUseCase.approveEntry(entryId, version));
    }

    @PutMapping("/entries/{entryId}/reject")
    public ResponseEntity<ApproveTimesheetUseCase.ApprovalResult> rejectEntry(
            @PathVariable Long entryId,
            @RequestBody Map<String, Object> request) {
        Long version = request.containsKey("version") ? Long.valueOf(request.get("version").toString()) : null;
        String reason = (String) request.get("rejectionReason");
        return ResponseEntity.ok(approveTimesheetUseCase.rejectEntry(entryId, version, reason));
    }

    @PutMapping("/entries/{entryId}/adjust")
    public ResponseEntity<AdjustApprovedWorkLogResult> adjustEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody AdjustApprovedWorkLogRequest request) {
        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                entryId,
                request.hours(),
                request.taskId(),
                request.isBillable(),
                request.description(),
                request.reason(),
                request.version()
        );
        return ResponseEntity.ok(adjustApprovedWorkLogUseCase.adjustApprovedWorkLog(command));
    }
}
