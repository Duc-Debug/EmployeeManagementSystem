package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.timesheet.UpdateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.UpdateWorkLogUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogDescriptionBlankException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInClosedProjectException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.user.UserId;

public class UpdateWorkLogService implements UpdateWorkLogUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final SaveTimesheetEntryPort saveTimesheetEntryPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final AuthorizationService authorizationService;

    public UpdateWorkLogService(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.loadTimesheetPort = Objects.requireNonNull(loadTimesheetPort, "LoadTimesheetPort must not be null");
        this.saveTimesheetPort = Objects.requireNonNull(saveTimesheetPort, "SaveTimesheetPort must not be null");
        this.loadTimesheetEntryPort = Objects.requireNonNull(loadTimesheetEntryPort, "LoadTimesheetEntryPort must not be null");
        this.saveTimesheetEntryPort = Objects.requireNonNull(saveTimesheetEntryPort, "SaveTimesheetEntryPort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public WorkLogResult updateWorkLog(UpdateWorkLogCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_UPDATE);

        Employee baseEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự của người dùng hiện tại"));

        // Concurrency-safe serialization: Lock employee row to prevent race condition on QTN-09 daily hours limit
        Employee employee = loadEmployeePort.findByIdForUpdate(baseEmployee.getId())
                .orElse(baseEmployee);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException("Tài khoản nhân sự không ở trạng thái hoạt động (ACTIVE).");
        }

        if (command.entryId() == null) {
            throw new TimesheetEntryNotFoundException("ID dòng ghi giờ không được để trống.");
        }

        TimesheetEntry entry = loadTimesheetEntryPort.findById(new TimesheetEntryId(command.entryId()))
                .orElseThrow(() -> new TimesheetEntryNotFoundException(command.entryId()));

        if (!Objects.equals(entry.getEmployeeIdValue(), employee.getIdValue())) {
            throw new WorkLogTaskNotAssignedException("Bạn không có quyền sửa dòng ghi giờ của người khác.");
        }

        Timesheet timesheet = loadTimesheetPort.findById(entry.getTimesheetId())
                .orElseThrow(() -> new TimesheetNotFoundException(entry.getTimesheetIdValue()));

        timesheet.assertModifiable();
        entry.assertModifiable();

        // 1. Validate Input Data
        if (command.workDate() == null) {
            throw new WorkLogInvalidHoursException("Ngày làm việc không được để trống.");
        }
        if (command.hours() == null) {
            throw new WorkLogInvalidHoursException("Số giờ làm việc không được để trống.");
        }
        if (command.hours().compareTo(BigDecimal.ZERO) <= 0 || command.hours().compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new WorkLogInvalidHoursException("Số giờ làm việc phải lớn hơn 0 và không vượt quá 24 giờ.");
        }
        if (command.description() == null || command.description().trim().isEmpty()) {
            throw new WorkLogDescriptionBlankException("Mô tả nội dung công việc không được để trống.");
        }
        if (command.projectId() == null) {
            throw new ProjectNotFoundException("Dự án không được để trống.");
        }
        if (command.taskId() == null) {
            throw new TaskNotFoundException("Công việc không được để trống.");
        }

        if (employee.getContractEndDate() != null && command.workDate().isAfter(employee.getContractEndDate())) {
            throw new WorkLogTaskNotAssignedException("Ngày làm việc " + command.workDate() + " vượt quá ngày kết thúc hợp đồng (" + employee.getContractEndDate() + ").");
        }

        // 2. Validate Project
        var project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new WorkLogInClosedProjectException("Không thể ghi giờ làm việc cho dự án đã đóng theo quy tắc QTN-08.");
        }

        // 3. Validate Task & Assignment (QTN-01)
        var task = loadTaskPort.findById(new TaskId(command.taskId()))
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        if (!Objects.equals(task.getProjectIdValue(), command.projectId())) {
            throw new TaskNotFoundException("Công việc không thuộc dự án đã chọn.");
        }

        if (task.getTaskType() == TaskType.CATEGORY) {
            throw new InvalidTaskDataException("Không thể ghi giờ làm việc cho hạng mục công việc (CATEGORY).");
        }

        boolean isAssigned = (task.getAssigneeId() != null && Objects.equals(task.getAssigneeId().value(), employee.getIdValue()))
                || loadTaskAssignmentPort.findByTaskIdAndEmployeeId(task.getId(), employee.getId()).isPresent();

        if (!isAssigned) {
            throw new WorkLogTaskNotAssignedException("Bạn chưa được phân công thực hiện công việc này.");
        }

        // 4. Validate QTN-09: Max 12 hours per day excluding current entry
        BigDecimal existingDayHours = loadTimesheetEntryPort.sumHoursByEmployeeAndDate(employee.getId(), command.workDate(), entry.getId());
        BigDecimal newTotalDayHours = existingDayHours.add(command.hours());
        if (newTotalDayHours.compareTo(BigDecimal.valueOf(12)) > 0) {
            throw new DailyHoursLimitExceededException(command.workDate(), existingDayHours, command.hours());
        }

        // 5. Check if workDate changed to another week -> move timesheet if needed
        LocalDate weekMonday = command.workDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (!Objects.equals(timesheet.getWeekStartDate(), weekMonday)) {
            Timesheet targetTimesheet = loadTimesheetPort.findByEmployeeAndWeekStart(employee.getId(), weekMonday)
                    .orElseGet(() -> {
                        Timesheet newTs = Timesheet.create(employee.getId(), command.workDate());
                        return saveTimesheetPort.save(newTs);
                    });
            targetTimesheet.assertModifiable();
            entry.assignTimesheetId(targetTimesheet.getId());
        }

        boolean isBillable = command.isBillable() == null || command.isBillable();
        entry.updateDetails(
                project.getId(),
                task.getId(),
                command.workDate(),
                command.hours(),
                isBillable,
                command.description()
        );

        TimesheetEntry savedEntry = saveTimesheetEntryPort.save(entry);

        // 6. Recalculate original and target timesheet hours
        recalculateTimesheet(entry.getTimesheetId());
        if (!Objects.equals(timesheet.getId(), entry.getTimesheetId())) {
            recalculateTimesheet(timesheet.getId());
        }

        // 7. Audit log
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.create(
                    currentUserId,
                    "UPDATE_WORK_LOG",
                    "timesheet_entries",
                    savedEntry.getIdValue()
            ));
        }

        return new WorkLogResult(
                savedEntry.getIdValue(),
                savedEntry.getTimesheetIdValue(),
                employee.getIdValue(),
                employee.getFullName(),
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                task.getIdValue(),
                task.getTaskCode(),
                task.getName(),
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
    }

    private void recalculateTimesheet(com.hrm.employeemanagement.domain.timesheet.TimesheetId tsId) {
        var tsOpt = loadTimesheetPort.findById(tsId);
        if (tsOpt.isPresent()) {
            Timesheet ts = tsOpt.get();
            var entries = loadTimesheetEntryPort.findByTimesheetId(ts.getId());
            BigDecimal totalHours = entries.stream()
                    .map(TimesheetEntry::getHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Timesheet updated = new Timesheet(
                    ts.getId(),
                    ts.getEmployeeId(),
                    ts.getWeekStartDate(),
                    ts.getWeekEndDate(),
                    totalHours,
                    ts.getStatus(),
                    ts.getSubmittedAt(),
                    ts.getApprovedBy(),
                    ts.getApprovedAt(),
                    ts.getRejectionReason(),
                    ts.getCreatedAt(),
                    java.time.LocalDateTime.now(),
                    ts.getVersion(),
                    entries
            );
            saveTimesheetPort.save(updated);
        }
    }
}
