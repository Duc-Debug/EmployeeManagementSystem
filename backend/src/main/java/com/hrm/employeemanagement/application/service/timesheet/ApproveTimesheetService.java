package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.ApproveTimesheetUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetAuditLog;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;

public class ApproveTimesheetService implements ApproveTimesheetUseCase {

    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final SaveTimesheetEntryPort saveTimesheetEntryPort;
    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveTimesheetAuditLogPort saveTimesheetAuditLogPort;
    private final AuthorizationService authorizationService;

    public ApproveTimesheetService(
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            SaveTimesheetAuditLogPort saveTimesheetAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadTimesheetEntryPort = loadTimesheetEntryPort;
        this.saveTimesheetEntryPort = saveTimesheetEntryPort;
        this.loadTimesheetPort = loadTimesheetPort;
        this.saveTimesheetPort = saveTimesheetPort;
        this.loadProjectPort = loadProjectPort;
        this.loadTaskPort = loadTaskPort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveTimesheetAuditLogPort = saveTimesheetAuditLogPort;
        this.authorizationService = authorizationService;
    }

    @Override
    public ApprovalResult approveEntry(Long entryId) {
        return processApproval(entryId, true, null);
    }

    @Override
    public ApprovalResult rejectEntry(Long entryId, String reason) {
        return processApproval(entryId, false, reason);
    }

    private ApprovalResult processApproval(Long entryId, boolean isApprove, String reason) {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_APPROVE);
        UserId actorId = new UserId(currentUserId);
        
        Employee currentEmployee = loadEmployeePort.findByUserId(actorId)
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân viên (PM)"));

        TimesheetEntry entryRef = loadTimesheetEntryPort.findById(new TimesheetEntryId(entryId))
                .orElseThrow(() -> new TimesheetEntryNotFoundException("Không tìm thấy dòng giờ công"));

        Timesheet timesheet = loadTimesheetPort.findById(entryRef.getTimesheetId())
                .orElseThrow(() -> new TimesheetNotFoundException("Không tìm thấy bảng chấm công"));
                
        List<TimesheetEntry> allEntries = loadTimesheetEntryPort.findByTimesheetId(timesheet.getId());
        timesheet.setEntries(allEntries);

        TimesheetEntry entry = timesheet.getEntries().stream()
                .filter(e -> e.getIdValue().equals(entryId))
                .findFirst()
                .orElseThrow(() -> new TimesheetEntryNotFoundException("Không tìm thấy dòng giờ công trong bảng chấm công"));

        Project project = loadProjectPort.findById(entry.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy dự án"));

        if (!project.isManagedBy(currentEmployee.getId())) {
            throw new PermissionDeniedException(PermissionCode.WORK_LOG_APPROVE);
        }

        List<String> warnings = new ArrayList<>();
        
        if (isApprove) {
            entry.approve();
            Task task = loadTaskPort.findById(entry.getTaskId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy công việc"));
            
            checkBudgetWarning(task, entry, warnings);
        } else {
            entry.reject(reason);
        }

        TimesheetEntry savedEntry = saveTimesheetEntryPort.save(entry);

        timesheet.syncStatusFromEntries(currentUserId);
        saveTimesheetPort.save(timesheet);

        // Save Audit Log
        TimesheetAuditLog auditLog = TimesheetAuditLog.create(
                timesheet.getId(),
                savedEntry.getId(),
                isApprove ? "APPROVE" : "REJECT",
                actorId,
                isApprove ? null : reason
        );
        saveTimesheetAuditLogPort.save(auditLog);

        Employee entryOwner = loadEmployeePort.findById(savedEntry.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên của dòng giờ công"));

        Task task = loadTaskPort.findById(savedEntry.getTaskId()).orElse(null);

        WorkLogResult result = new WorkLogResult(
                savedEntry.getIdValue(),
                savedEntry.getTimesheetIdValue(),
                entryOwner.getIdValue(),
                entryOwner.getFullName(),
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                savedEntry.getTaskIdValue(),
                task != null ? task.getTaskCode() : "-",
                task != null ? task.getName() : "-",
                savedEntry.getWorkDate(),
                savedEntry.getHours(),
                savedEntry.isBillable(),
                savedEntry.getDescription(),
                savedEntry.getStatus().name(),
                savedEntry.getRejectionReason(),
                savedEntry.getCreatedAt(),
                savedEntry.getUpdatedAt(),
                savedEntry.getVersion()
        );

        return new ApprovalResult(result, warnings);
    }

    private void checkBudgetWarning(Task task, TimesheetEntry entry, List<String> warnings) {
        if (task.getEstimatedHours() != null && task.getEstimatedHours().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actual = task.getActualHours() != null ? task.getActualHours() : BigDecimal.ZERO;
            BigDecimal entryHours = entry.getHours() != null ? entry.getHours() : BigDecimal.ZERO;
            
            BigDecimal projectedTotal = actual.add(entryHours);
            BigDecimal threshold = task.getEstimatedHours().multiply(new BigDecimal("0.8"));
            
            if (projectedTotal.compareTo(threshold) >= 0) {
                BigDecimal percentage = projectedTotal.multiply(new BigDecimal("100"))
                        .divide(task.getEstimatedHours(), 0, java.math.RoundingMode.HALF_UP);
                
                if (projectedTotal.compareTo(task.getEstimatedHours()) > 0) {
                    warnings.add("Thời gian thực tế (" + projectedTotal + "h) đã VƯỢT quỹ thời gian (" + task.getEstimatedHours() + "h) - Đạt " + percentage + "%.");
                } else {
                    warnings.add("Thời gian thực tế (" + projectedTotal + "h) đã đạt " + percentage + "% quỹ thời gian (" + task.getEstimatedHours() + "h).");
                }
            }
        }
    }
}
