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

import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ApproveTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetPendingApprovalsUseCase;

@RestController
@RequestMapping("/api/v1/work-logs/approvals")
public class TimesheetApprovalController {

    private final ApproveTimesheetUseCase approveTimesheetUseCase;
    private final GetPendingApprovalsUseCase getPendingApprovalsUseCase;

    public TimesheetApprovalController(
            ApproveTimesheetUseCase approveTimesheetUseCase,
            GetPendingApprovalsUseCase getPendingApprovalsUseCase) {
        this.approveTimesheetUseCase = approveTimesheetUseCase;
        this.getPendingApprovalsUseCase = getPendingApprovalsUseCase;
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
}
