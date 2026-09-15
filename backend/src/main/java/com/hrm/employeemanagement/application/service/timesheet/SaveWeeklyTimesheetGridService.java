package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.timesheet.SaveWeeklyTimesheetGridCommand;
import com.hrm.employeemanagement.application.dto.timesheet.TaskDailyHourInputDto;
import com.hrm.employeemanagement.application.dto.timesheet.TaskWeeklyHoursInputDto;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SaveWeeklyTimesheetGridUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.DeleteTimesheetEntryPort;
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
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInClosedProjectException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;

public class SaveWeeklyTimesheetGridService implements SaveWeeklyTimesheetGridUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final SaveTimesheetEntryPort saveTimesheetEntryPort;
    private final DeleteTimesheetEntryPort deleteTimesheetEntryPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase;
    private final AuthorizationService authorizationService;

    public SaveWeeklyTimesheetGridService(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            DeleteTimesheetEntryPort deleteTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase,
            AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.loadTimesheetPort = Objects.requireNonNull(loadTimesheetPort, "LoadTimesheetPort must not be null");
        this.saveTimesheetPort = Objects.requireNonNull(saveTimesheetPort, "SaveTimesheetPort must not be null");
        this.loadTimesheetEntryPort = Objects.requireNonNull(loadTimesheetEntryPort, "LoadTimesheetEntryPort must not be null");
        this.saveTimesheetEntryPort = Objects.requireNonNull(saveTimesheetEntryPort, "SaveTimesheetEntryPort must not be null");
        this.deleteTimesheetEntryPort = Objects.requireNonNull(deleteTimesheetEntryPort, "DeleteTimesheetEntryPort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
        this.getWeeklyTimesheetUseCase = Objects.requireNonNull(getWeeklyTimesheetUseCase, "GetWeeklyTimesheetUseCase must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public WeeklyTimesheetResult saveWeeklyGrid(SaveWeeklyTimesheetGridCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_CREATE);

        Employee baseEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự của người dùng hiện tại"));

        Employee employee = loadEmployeePort.findByIdForUpdate(baseEmployee.getId())
                .orElse(baseEmployee);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException("Tài khoản nhân sự không ở trạng thái hoạt động (ACTIVE).");
        }

        LocalDate targetDate = command.dateInWeek() != null ? command.dateInWeek() : LocalDate.now();
        LocalDate weekMonday = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekSunday = targetDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 1. Get or Create Timesheet
        Timesheet timesheet = loadTimesheetPort.findByEmployeeAndWeekStart(employee.getId(), weekMonday)
                .orElseGet(() -> {
                    Timesheet newTs = Timesheet.create(employee.getId(), weekMonday);
                    return saveTimesheetPort.save(newTs);
                });

        // Validate QTN-07: Timesheet immutability if SUBMITTED or APPROVED
        timesheet.assertModifiable();

        List<TimesheetEntry> existingEntries = loadTimesheetEntryPort.findByEmployeeAndDateRange(
                employee.getId(), weekMonday, weekSunday);

        // Map existing entries by (taskId, workDate)
        Map<String, TimesheetEntry> existingEntryMap = new HashMap<>();
        for (TimesheetEntry entry : existingEntries) {
            String key = entry.getTaskIdValue() + "_" + entry.getWorkDate();
            existingEntryMap.put(key, entry);
        }

        // 2. Validate Projects, Tasks, and Calculate Daily Totals
        Map<LocalDate, BigDecimal> dailyTotals = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyTotals.put(weekMonday.plusDays(i), BigDecimal.ZERO);
        }
        for (TimesheetEntry entry : existingEntries) {
            if (entry.getWorkDate() != null && entry.getHours() != null) {
                dailyTotals.put(entry.getWorkDate(),
                        dailyTotals.getOrDefault(entry.getWorkDate(), BigDecimal.ZERO).add(entry.getHours()));
            }
        }

        if (command.taskEntries() != null) {
            for (TaskWeeklyHoursInputDto row : command.taskEntries()) {
                if (row.projectId() == null) {
                    throw new ProjectNotFoundException("Dự án không được để trống.");
                }
                if (row.taskId() == null) {
                    throw new TaskNotFoundException("Công việc không được để trống.");
                }

                // Validate Project (QTN-08)
                var project = loadProjectPort.findById(new ProjectId(row.projectId()))
                        .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + row.projectId()));

                if (project.getStatus() == ProjectStatus.CLOSED) {
                    throw new WorkLogInClosedProjectException("Không thể ghi giờ làm việc cho dự án đã đóng theo quy tắc QTN-08.");
                }

                // Validate Task & Assignment
                Task task = loadTaskPort.findById(new TaskId(row.taskId()))
                        .orElseThrow(() -> new TaskNotFoundException(row.taskId()));

                if (!Objects.equals(task.getProjectIdValue(), row.projectId())) {
                    throw new TaskNotFoundException("Công việc không thuộc dự án đã chọn.");
                }

                if (task.getTaskType() == TaskType.CATEGORY) {
                    throw new InvalidTaskDataException("Không thể ghi giờ làm việc cho hạng mục công việc (CATEGORY).");
                }

                boolean isAssigned = (task.getAssigneeId() != null && Objects.equals(task.getAssigneeId().value(), employee.getIdValue()))
                        || loadTaskAssignmentPort.findByTaskIdAndEmployeeId(task.getId(), employee.getId()).isPresent();

                if (!isAssigned) {
                    throw new WorkLogTaskNotAssignedException("Bạn chưa được phân công thực hiện công việc: " + task.getName());
                }

                // Process daily hours
                if (row.dailyHours() != null) {
                    for (TaskDailyHourInputDto dayInput : row.dailyHours()) {
                        if (dayInput.workDate() == null) continue;

                        if (dayInput.workDate().isBefore(weekMonday) || dayInput.workDate().isAfter(weekSunday)) {
                            throw new WorkLogInvalidHoursException("Ngày làm việc " + dayInput.workDate() + " nằm ngoài tuần đang chọn.");
                        }

                        if (employee.getContractEndDate() != null && dayInput.workDate().isAfter(employee.getContractEndDate())) {
                            throw new WorkLogTaskNotAssignedException("Ngày làm việc " + dayInput.workDate() + " vượt quá ngày kết thúc hợp đồng.");
                        }

                        BigDecimal hours = dayInput.hours();
                        if (hours != null && hours.compareTo(BigDecimal.ZERO) > 0) {
                            if (hours.compareTo(BigDecimal.valueOf(24)) > 0) {
                                throw new WorkLogInvalidHoursException("Số giờ làm việc không được vượt quá 24 giờ.");
                            }
                        }

                        String key = row.taskId() + "_" + dayInput.workDate();
                        TimesheetEntry existing = existingEntryMap.get(key);

                        BigDecimal oldHours = (existing != null && existing.getHours() != null)
                                ? existing.getHours()
                                : BigDecimal.ZERO;
                        BigDecimal newHours = (hours != null && hours.compareTo(BigDecimal.ZERO) > 0)
                                ? hours
                                : BigDecimal.ZERO;

                        BigDecimal currentSimulated = dailyTotals.getOrDefault(dayInput.workDate(), BigDecimal.ZERO);
                        dailyTotals.put(dayInput.workDate(), currentSimulated.subtract(oldHours).add(newHours));
                    }
                }
            }
        }

        // 3. Validate QTN-09: Max 12 hours per day
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyTotals.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.valueOf(12)) > 0) {
                throw new DailyHoursLimitExceededException(entry.getKey(), BigDecimal.ZERO, entry.getValue());
            }
        }

        // 4. Save/Update/Delete entries
        if (command.taskEntries() != null) {
            for (TaskWeeklyHoursInputDto row : command.taskEntries()) {
                boolean isBillable = row.isBillable() == null || row.isBillable();
                String rowDesc = (row.description() != null && !row.description().trim().isEmpty())
                        ? row.description().trim()
                        : "Ghi giờ nhanh theo tuần";

                if (row.dailyHours() != null) {
                    for (TaskDailyHourInputDto dayInput : row.dailyHours()) {
                        if (dayInput.workDate() == null) continue;

                        String key = row.taskId() + "_" + dayInput.workDate();
                        TimesheetEntry existing = existingEntryMap.get(key);
                        BigDecimal hours = dayInput.hours();

                        if (hours != null && hours.compareTo(BigDecimal.ZERO) > 0) {
                            if (existing != null) {
                                // Update existing entry
                                existing.updateDetails(new ProjectId(row.projectId()), new TaskId(row.taskId()), dayInput.workDate(), hours, isBillable, rowDesc);
                                saveTimesheetEntryPort.save(existing);
                            } else {
                                // Create new entry
                                TimesheetEntry newEntry = TimesheetEntry.create(
                                        timesheet.getId(),
                                        employee.getId(),
                                        new ProjectId(row.projectId()),
                                        new TaskId(row.taskId()),
                                        dayInput.workDate(),
                                        hours,
                                        isBillable,
                                        rowDesc
                                );
                                saveTimesheetEntryPort.save(newEntry);
                            }
                        } else if (existing != null && existing.getStatus() == TimesheetStatus.DRAFT) {
                            // Clear 0h / empty cell by deleting DRAFT entry
                            deleteTimesheetEntryPort.deleteById(existing.getId());
                        }
                    }
                }
            }
        }

        // 5. Recalculate Timesheet Total Hours
        List<TimesheetEntry> updatedEntries = loadTimesheetEntryPort.findByTimesheetId(timesheet.getId());
        BigDecimal newTotalHours = updatedEntries.stream()
                .map(TimesheetEntry::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Timesheet updatedTimesheet = new Timesheet(
                timesheet.getId(),
                timesheet.getEmployeeId(),
                timesheet.getWeekStartDate(),
                timesheet.getWeekEndDate(),
                newTotalHours,
                timesheet.getStatus(),
                timesheet.getSubmittedAt(),
                timesheet.getApprovedBy(),
                timesheet.getApprovedAt(),
                timesheet.getRejectionReason(),
                timesheet.getCreatedAt(),
                java.time.LocalDateTime.now(),
                timesheet.getVersion(),
                updatedEntries
        );
        saveTimesheetPort.save(updatedTimesheet);

        // 6. Audit Trail Log (NCL-09-CN-007-TC-04)
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.create(
                    currentUserId,
                    "SAVE_WEEKLY_GRID",
                    "timesheets",
                    timesheet.getIdValue()
            ));
        }

        return getWeeklyTimesheetUseCase.getMyWeeklyTimesheet(weekMonday);
    }
}
