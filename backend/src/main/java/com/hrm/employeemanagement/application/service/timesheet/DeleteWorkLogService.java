package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.timesheet.DeleteWorkLogCommand;
import com.hrm.employeemanagement.application.port.inbound.timesheet.DeleteWorkLogUseCase;
import com.hrm.employeemanagement.application.port.outbound.timesheet.DeleteTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.user.UserId;

public class DeleteWorkLogService implements DeleteWorkLogUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final DeleteTimesheetEntryPort deleteTimesheetEntryPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final AuthorizationService authorizationService;

    public DeleteWorkLogService(
            LoadEmployeePort loadEmployeePort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            DeleteTimesheetEntryPort deleteTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadTimesheetPort = Objects.requireNonNull(loadTimesheetPort, "LoadTimesheetPort must not be null");
        this.saveTimesheetPort = Objects.requireNonNull(saveTimesheetPort, "SaveTimesheetPort must not be null");
        this.loadTimesheetEntryPort = Objects.requireNonNull(loadTimesheetEntryPort, "LoadTimesheetEntryPort must not be null");
        this.deleteTimesheetEntryPort = Objects.requireNonNull(deleteTimesheetEntryPort, "DeleteTimesheetEntryPort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public void deleteWorkLog(DeleteWorkLogCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_DELETE);

        Employee employee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự của người dùng hiện tại"));

        if (command.entryId() == null) {
            throw new TimesheetEntryNotFoundException("ID dòng ghi giờ không được để trống.");
        }

        TimesheetEntry entry = loadTimesheetEntryPort.findById(new TimesheetEntryId(command.entryId()))
                .orElseThrow(() -> new TimesheetEntryNotFoundException(command.entryId()));

        if (!Objects.equals(entry.getEmployeeIdValue(), employee.getIdValue())) {
            throw new WorkLogTaskNotAssignedException("Bạn không có quyền xóa dòng ghi giờ của người khác.");
        }

        Timesheet timesheet = loadTimesheetPort.findById(entry.getTimesheetId())
                .orElseThrow(() -> new TimesheetNotFoundException(entry.getTimesheetIdValue()));

        timesheet.assertModifiable();

        TimesheetId timesheetId = entry.getTimesheetId();
        deleteTimesheetEntryPort.deleteById(entry.getId());

        // Recalculate timesheet total hours
        var remainingEntries = loadTimesheetEntryPort.findByTimesheetId(timesheetId);
        BigDecimal totalHours = remainingEntries.stream()
                .map(TimesheetEntry::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Timesheet updated = new Timesheet(
                timesheet.getId(),
                timesheet.getEmployeeId(),
                timesheet.getWeekStartDate(),
                timesheet.getWeekEndDate(),
                totalHours,
                timesheet.getStatus(),
                timesheet.getSubmittedAt(),
                timesheet.getApprovedBy(),
                timesheet.getApprovedAt(),
                timesheet.getRejectionReason(),
                timesheet.getCreatedAt(),
                java.time.LocalDateTime.now(),
                timesheet.getVersion(),
                remainingEntries
        );
        saveTimesheetPort.save(updated);

        // Audit Log
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.create(
                    currentUserId,
                    "DELETE_WORK_LOG",
                    "timesheet_entries",
                    entry.getIdValue()
            ));
        }
    }
}
