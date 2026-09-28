package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.timesheet.CreateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.CreateWorkLogUseCase;
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
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
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
import com.hrm.employeemanagement.domain.user.UserId;

public class CreateWorkLogService implements CreateWorkLogUseCase {

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

    public CreateWorkLogService(
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
    public WorkLogResult createWorkLog(CreateWorkLogCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_CREATE);

        Employee baseEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự của người dùng hiện tại"));

        // Concurrency-safe serialization: Lock employee row to prevent race condition on QTN-09 daily hours limit
        Employee employee = loadEmployeePort.findByIdForUpdate(baseEmployee.getId())
                .orElse(baseEmployee);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException("Tài khoản nhân sự không ở trạng thái hoạt động (ACTIVE).");
        }

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
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidProjectDataException("Chỉ có thể ghi giờ làm việc cho dự án đang ở trạng thái hoạt động (Trạng thái hiện tại: " + project.getStatus() + ").");
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

        // 4. Validate QTN-09: Max 12 hours per day
        BigDecimal existingDayHours = loadTimesheetEntryPort.sumHoursByEmployeeAndDate(employee.getId(), command.workDate(), null);
        BigDecimal newTotalDayHours = existingDayHours.add(command.hours());
        if (newTotalDayHours.compareTo(BigDecimal.valueOf(12)) > 0) {
            throw new DailyHoursLimitExceededException(command.workDate(), existingDayHours, command.hours());
        }

        // 5. Load or Create Weekly Timesheet
        LocalDate weekMonday = command.workDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Timesheet timesheet = loadTimesheetPort.findByEmployeeAndWeekStart(employee.getId(), weekMonday)
                .orElseGet(() -> {
                    Timesheet newTs = Timesheet.create(employee.getId(), command.workDate());
                    return saveTimesheetPort.save(newTs);
                });

        timesheet.assertModifiable();

        // 6. Create Timesheet Entry
        boolean isBillable = command.isBillable() == null || command.isBillable();
        TimesheetEntry entry = TimesheetEntry.create(
                timesheet.getId(),
                employee.getId(),
                project.getId(),
                task.getId(),
                command.workDate(),
                command.hours(),
                isBillable,
                command.description()
        );

        TimesheetEntry savedEntry = saveTimesheetEntryPort.save(entry);

        // 7. Update Timesheet total hours
        var entries = loadTimesheetEntryPort.findByTimesheetId(timesheet.getId());
        BigDecimal totalHours = entries.stream()
                .map(TimesheetEntry::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Timesheet updatedTimesheet = new Timesheet(
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
                entries
        );
        saveTimesheetPort.save(updatedTimesheet);

        // 8. Audit Log (TC-05)
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.create(
                    currentUserId,
                    "CREATE_WORK_LOG",
                    "timesheet_entries",
                    savedEntry.getIdValue()
            ));
        }

        return new WorkLogResult(
                savedEntry.getIdValue(),
                timesheet.getIdValue(),
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
}
