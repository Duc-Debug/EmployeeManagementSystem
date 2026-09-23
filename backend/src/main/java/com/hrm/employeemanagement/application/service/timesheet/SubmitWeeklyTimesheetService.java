package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.timesheet.DailyWorkLogGroupDto;
import com.hrm.employeemanagement.application.dto.timesheet.SubmitWeeklyTimesheetCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.SubmitWeeklyTimesheetUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
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
import com.hrm.employeemanagement.domain.exception.timesheet.EmptyTimesheetSubmissionException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.user.UserId;

public class SubmitWeeklyTimesheetService implements SubmitWeeklyTimesheetUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final SaveTimesheetEntryPort saveTimesheetEntryPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final AuthorizationService authorizationService;

    public SubmitWeeklyTimesheetService(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadTimesheetPort = Objects.requireNonNull(loadTimesheetPort, "LoadTimesheetPort must not be null");
        this.saveTimesheetPort = Objects.requireNonNull(saveTimesheetPort, "SaveTimesheetPort must not be null");
        this.loadTimesheetEntryPort = Objects.requireNonNull(loadTimesheetEntryPort, "LoadTimesheetEntryPort must not be null");
        this.saveTimesheetEntryPort = Objects.requireNonNull(saveTimesheetEntryPort, "SaveTimesheetEntryPort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public WeeklyTimesheetResult submitWeeklyTimesheet(SubmitWeeklyTimesheetCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_UPDATE);

        Employee baseEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự của người dùng hiện tại"));

        // Concurrency serialization lock: Uses SELECT ... FOR UPDATE within active @Transactional scope
        Employee employee = loadEmployeePort.findByIdForUpdate(baseEmployee.getId())
                .orElse(baseEmployee);

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException("Tài khoản nhân sự không ở trạng thái hoạt động (ACTIVE).");
        }

        Timesheet timesheet;
        if (command != null && command.timesheetId() != null) {
            timesheet = loadTimesheetPort.findById(new TimesheetId(command.timesheetId()))
                    .orElseThrow(() -> new TimesheetNotFoundException(command.timesheetId()));
        } else {
            LocalDate targetDate = (command != null && command.dateInWeek() != null) ? command.dateInWeek() : LocalDate.now();
            LocalDate computedMonday = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            timesheet = loadTimesheetPort.findByEmployeeAndWeekStart(employee.getId(), computedMonday)
                    .orElseThrow(() -> new EmptyTimesheetSubmissionException("Tuần này chưa có bảng chấm công nào để nộp."));
        }

        if (!Objects.equals(timesheet.getEmployeeIdValue(), employee.getIdValue())) {
            throw new WorkLogTaskNotAssignedException("Bạn không có quyền nộp bảng chấm công của nhân sự khác.");
        }

        // Always derive the week start & end dates from the timesheet entity itself
        LocalDate weekMonday = timesheet.getWeekStartDate();
        LocalDate weekSunday = timesheet.getWeekEndDate() != null
                ? timesheet.getWeekEndDate()
                : weekMonday.plusDays(6);

        List<TimesheetEntry> entries = loadTimesheetEntryPort.findByTimesheetId(timesheet.getId());
        if (entries == null || entries.isEmpty()) {
            throw new EmptyTimesheetSubmissionException("Không thể nộp bảng chấm công rỗng. Vui lòng ghi nhận ít nhất một dòng giờ công.");
        }

        timesheet.setEntries(entries);
        timesheet.submit();

        Timesheet savedTimesheet = saveTimesheetPort.save(timesheet);
        for (TimesheetEntry entry : timesheet.getEntries()) {
            saveTimesheetEntryPort.save(entry);
        }

        // TC-05: Audit log
        if (saveAuditLogPort != null) {
            saveAuditLogPort.save(AuditLog.create(
                    currentUserId,
                    "SUBMIT_TIMESHEET",
                    "timesheets",
                    savedTimesheet.getIdValue()
            ));
        }

        return buildWeeklyTimesheetResult(employee, savedTimesheet, timesheet.getEntries(), weekMonday, weekSunday);
    }

    private WeeklyTimesheetResult buildWeeklyTimesheetResult(
            Employee employee,
            Timesheet timesheet,
            List<TimesheetEntry> entries,
            LocalDate weekMonday,
            LocalDate weekSunday) {

        List<ProjectId> distinctProjIds = entries.stream()
                .map(e -> new ProjectId(e.getProjectIdValue()))
                .distinct()
                .toList();
        Map<Long, com.hrm.employeemanagement.domain.project.Project> projectMap = distinctProjIds.isEmpty()
                ? Map.of()
                : loadProjectPort.findAllById(distinctProjIds).stream()
                        .collect(Collectors.toMap(p -> p.getId().value(), p -> p, (a, b) -> a));

        List<TaskId> distinctTaskIds = entries.stream()
                .map(e -> new TaskId(e.getTaskIdValue()))
                .distinct()
                .toList();
        Map<Long, com.hrm.employeemanagement.domain.task.Task> taskMap = distinctTaskIds.isEmpty()
                ? Map.of()
                : loadTaskPort.findAllById(distinctTaskIds).stream()
                        .collect(Collectors.toMap(t -> t.getId().value(), t -> t, (a, b) -> a));

        List<WorkLogResult> resultEntries = entries.stream().map(e -> {
            var project = projectMap.get(e.getProjectIdValue());
            String projectCode = project != null ? project.getProjectCode() : "-";
            String projectName = project != null ? project.getProjectName() : "-";

            var task = taskMap.get(e.getTaskIdValue());
            String taskCode = task != null ? task.getTaskCode() : "-";
            String taskName = task != null ? task.getName() : "-";

            return new WorkLogResult(
                    e.getIdValue(),
                    e.getTimesheetIdValue(),
                    employee.getIdValue(),
                    employee.getFullName(),
                    e.getProjectIdValue(),
                    projectCode,
                    projectName,
                    e.getTaskIdValue(),
                    taskCode,
                    taskName,
                    e.getWorkDate(),
                    e.getHours(),
                    e.isBillable(),
                    e.getDescription(),
                    e.getStatus().name(),
                    e.getRejectionReason(),
                    e.getCreatedAt(),
                    e.getUpdatedAt(),
                    e.getVersion()
            );
        }).toList();

        // Group by 7 days
        Map<LocalDate, List<WorkLogResult>> entriesByDate = resultEntries.stream()
                .collect(Collectors.groupingBy(WorkLogResult::workDate));

        List<DailyWorkLogGroupDto> dailyGroups = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 0; i < 7; i++) {
            LocalDate d = weekMonday.plusDays(i);
            List<WorkLogResult> dayEntries = entriesByDate.getOrDefault(d, List.of());
            BigDecimal dayTotal = dayEntries.stream()
                    .map(WorkLogResult::hours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean isExceeded = dayTotal.compareTo(BigDecimal.valueOf(12)) > 0;

            String dayLabel = getVietnameseDayOfWeek(d.getDayOfWeek()) + " (" + d.format(dayFormatter) + ")";
            dailyGroups.add(new DailyWorkLogGroupDto(d, dayLabel, dayTotal, isExceeded, dayEntries));
        }

        BigDecimal weekTotal = resultEntries.stream()
                .map(WorkLogResult::hours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String status = timesheet != null ? timesheet.getStatus().name() : "DRAFT";
        boolean isEditable = timesheet == null || timesheet.getStatus() == com.hrm.employeemanagement.domain.timesheet.TimesheetStatus.DRAFT;

        return new WeeklyTimesheetResult(
                timesheet != null ? timesheet.getIdValue() : null,
                employee.getIdValue(),
                employee.getFullName(),
                weekMonday,
                weekSunday,
                weekTotal,
                status,
                isEditable,
                dailyGroups,
                resultEntries
        );
    }

    private String getVietnameseDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }
}