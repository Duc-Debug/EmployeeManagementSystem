package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogResult;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
import com.hrm.employeemanagement.application.port.inbound.timesheet.AdjustApprovedWorkLogUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.SaveTimesheetPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryVersionConflictException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotApprovedException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotFoundException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogAdjustmentReasonRequiredException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetAuditLog;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;

public class AdjustApprovedWorkLogService implements AdjustApprovedWorkLogUseCase {

    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final SaveTimesheetEntryPort saveTimesheetEntryPort;
    private final LoadTimesheetPort loadTimesheetPort;
    private final SaveTimesheetPort saveTimesheetPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveTimesheetAuditLogPort saveTimesheetAuditLogPort;
    private final AuthorizationService authorizationService;
    private final CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase;

    public AdjustApprovedWorkLogService(
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            SaveTimesheetEntryPort saveTimesheetEntryPort,
            LoadTimesheetPort loadTimesheetPort,
            SaveTimesheetPort saveTimesheetPort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadEmployeePort loadEmployeePort,
            SaveTimesheetAuditLogPort saveTimesheetAuditLogPort,
            AuthorizationService authorizationService,
            CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase) {
        this.loadTimesheetEntryPort = Objects.requireNonNull(loadTimesheetEntryPort, "LoadTimesheetEntryPort must not be null");
        this.saveTimesheetEntryPort = Objects.requireNonNull(saveTimesheetEntryPort, "SaveTimesheetEntryPort must not be null");
        this.loadTimesheetPort = Objects.requireNonNull(loadTimesheetPort, "LoadTimesheetPort must not be null");
        this.saveTimesheetPort = Objects.requireNonNull(saveTimesheetPort, "SaveTimesheetPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveTimesheetAuditLogPort = Objects.requireNonNull(saveTimesheetAuditLogPort, "SaveTimesheetAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.checkAllocationPeriodLockUseCase = checkAllocationPeriodLockUseCase;
    }

    @Override
    public AdjustApprovedWorkLogResult adjustApprovedWorkLog(AdjustApprovedWorkLogCommand command) {
        // 1. Phân quyền: Cần quyền WORK_LOG_ADJUST hoặc WORK_LOG_APPROVE
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.WORK_LOG_ADJUST,
                PermissionCode.WORK_LOG_APPROVE
        );
        UserId actorId = new UserId(currentUserId);

        Employee currentEmployee = loadEmployeePort.findByUserId(actorId)
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân viên thao tác"));

        if (command.entryId() == null) {
            throw new TimesheetEntryNotFoundException("ID dòng ghi giờ không được để trống.");
        }

        // 2. Kiểm tra lý do giải trình bắt buộc (tối thiểu 10 ký tự)
        if (command.reason() == null || command.reason().trim().length() < 10) {
            throw new WorkLogAdjustmentReasonRequiredException(
                    "Lý do điều chỉnh không được để trống và phải có ít nhất 10 ký tự giải trình.");
        }

        // 3. Tải dòng ghi giờ cần điều chỉnh
        TimesheetEntry entry = loadTimesheetEntryPort.findById(new TimesheetEntryId(command.entryId()))
                .orElseThrow(() -> new TimesheetEntryNotFoundException(command.entryId()));

        // 4. Kiểm tra trạng thái: chỉ cho phép dòng APPROVED
        if (entry.getStatus() != TimesheetStatus.APPROVED) {
            throw new TimesheetNotApprovedException(
                    "Chỉ có thể điều chỉnh dòng giờ công đã được duyệt (APPROVED). Trạng thái hiện tại: [" + entry.getStatus() + "].");
        }

        // 5. Kiểm tra phiên bản Optimistic Locking
        if (command.version() == null) {
            throw new IllegalArgumentException("Phiên bản (version) không được để trống.");
        }
        if (entry.getVersion() != null && !entry.getVersion().equals(command.version())) {
            throw new TimesheetEntryVersionConflictException(
                    "Dòng giờ công đã bị thay đổi bởi người khác. Vui lòng tải lại trang và thử lại.");
        }

        // 6. Kiểm tra dự án và quyền sở hữu (PM phụ trách hoặc Admin có quyền WORK_LOG_ADJUST)
        Project project = loadProjectPort.findById(entry.getProjectId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy dự án của dòng ghi giờ"));

        boolean isManager = project.isManagedBy(currentEmployee.getId());
        boolean hasAdjustPerm = authorizationService.hasPermission(PermissionCode.WORK_LOG_ADJUST);
        if (!isManager && !hasAdjustPerm) {
            throw new PermissionDeniedException(PermissionCode.WORK_LOG_ADJUST);
        }

        // 7. Ràng buộc khóa kỳ (Period Lock)
        if (checkAllocationPeriodLockUseCase != null && entry.getWorkDate() != null) {
            int year = entry.getWorkDate().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR);
            int weekNumber = entry.getWorkDate().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            checkAllocationPeriodLockUseCase.validateWeekNotLocked(year, weekNumber);
        }

        // 8. Kiểm tra Task hợp lệ
        TaskId sourceTaskId = entry.getTaskId();
        TaskId targetTaskId = command.taskId() != null ? new TaskId(command.taskId()) : sourceTaskId;

        Task sourceTask;
        Task targetTask;

        if (sourceTaskId.equals(targetTaskId)) {
            targetTask = loadTaskPort.findById(sourceTaskId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy công việc"));
            if (!Objects.equals(targetTask.getProjectIdValue(), project.getIdValue())) {
                throw new IllegalArgumentException("Công việc không thuộc dự án của dòng ghi giờ.");
            }
            if (targetTask.getTaskType() == TaskType.CATEGORY) {
                throw new IllegalArgumentException("Không thể ghi nhận giờ công cho hạng mục công việc (CATEGORY).");
            }
            sourceTask = targetTask;
        } else {
            sourceTask = loadTaskPort.findById(sourceTaskId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy công việc gốc"));
            targetTask = loadTaskPort.findById(targetTaskId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy công việc mới"));
            if (!Objects.equals(targetTask.getProjectIdValue(), project.getIdValue())) {
                throw new IllegalArgumentException("Công việc không thuộc dự án của dòng ghi giờ.");
            }
            if (targetTask.getTaskType() == TaskType.CATEGORY) {
                throw new IllegalArgumentException("Không thể ghi nhận giờ công cho hạng mục công việc (CATEGORY).");
            }
        }

        // 9. Kiểm tra giới hạn số giờ và giới hạn 12h/ngày (QTN-09)
        BigDecimal newHours = command.hours() != null ? command.hours() : entry.getHours();
        if (newHours.compareTo(BigDecimal.ZERO) <= 0 || newHours.compareTo(BigDecimal.valueOf(24)) > 0) {
            throw new WorkLogInvalidHoursException("Số giờ làm việc phải lớn hơn 0 và không vượt quá 24 giờ.");
        }

        // Serialize every write that affects this employee's daily aggregate. Locking
        // only the entry cannot protect two concurrent edits of different entries.
        loadEmployeePort.findByIdForUpdate(entry.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên của dòng giờ công"));

        BigDecimal existingDayHours = loadTimesheetEntryPort.sumHoursByEmployeeAndDate(
                entry.getEmployeeId(),
                entry.getWorkDate(),
                entry.getId()
        );
        BigDecimal newTotalDayHours = existingDayHours.add(newHours);
        if (newTotalDayHours.compareTo(BigDecimal.valueOf(12)) > 0) {
            throw new DailyHoursLimitExceededException(entry.getWorkDate(), existingDayHours, newHours);
        }

        // 10. Điều chỉnh entry
        BigDecimal oldHours = entry.getHours();
        boolean oldBillable = entry.isBillable();
        String oldDescription = entry.getDescription();
        entry.adjustApproved(
                targetTaskId,
                newHours,
                command.billable(),
                command.description()
        );
        TimesheetEntry savedEntry = saveTimesheetEntryPort.save(entry);

        // 11. Đồng bộ Task.actualHours và kiểm tra ngân sách
        if (sourceTaskId.equals(targetTaskId)) {
            targetTask.subtractActualHours(oldHours);
            targetTask.addActualHours(newHours);
            saveTaskPort.save(targetTask);
        } else {
            sourceTask.subtractActualHours(oldHours);
            saveTaskPort.save(sourceTask);

            targetTask.addActualHours(newHours);
            saveTaskPort.save(targetTask);
        }

        List<String> warnings = new ArrayList<>();
        checkBudgetWarning(targetTask, warnings);

        // 12. Tính toán lại tổng giờ tuần của Timesheet
        Timesheet timesheet = loadTimesheetPort.findById(entry.getTimesheetId())
                .orElseThrow(() -> new TimesheetNotFoundException("Không tìm thấy bảng chấm công"));
        List<TimesheetEntry> allEntries = loadTimesheetEntryPort.findByTimesheetId(timesheet.getId());
        timesheet.setEntries(allEntries);
        timesheet.recalculateTotalHours();
        timesheet.syncStatusFromEntries(currentUserId);
        saveTimesheetPort.save(timesheet);

        // 13. Lưu vết kiểm toán (Audit Log)
        // Include every editable field, even when unchanged, so billable-only or
        // description-only adjustments remain fully reconstructable from the audit.
        String auditDetail = String.format(
                "[ĐIỀU CHỈNH GIỜ ĐÃ DUYỆT] Giờ: %sh -> %sh | Công việc: %s -> %s | Tính phí: %s -> %s | Mô tả: %s -> %s | Lý do: %s",
                oldHours, newHours,
                sourceTask.getTaskCode(), targetTask.getTaskCode(),
                oldBillable, savedEntry.isBillable(),
                auditValue(oldDescription), auditValue(savedEntry.getDescription()),
                command.reason().trim());

        TimesheetAuditLog auditLog = TimesheetAuditLog.create(
                timesheet.getId(),
                savedEntry.getId(),
                "ADJUST_APPROVED",
                actorId,
                auditDetail
        );
        saveTimesheetAuditLogPort.save(auditLog);

        // 14. Trả về kết quả
        Employee entryOwner = loadEmployeePort.findById(savedEntry.getEmployeeId())
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân viên của dòng giờ công"));

        WorkLogResult result = new WorkLogResult(
                savedEntry.getIdValue(),
                savedEntry.getTimesheetIdValue(),
                entryOwner.getIdValue(),
                entryOwner.getFullName(),
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                savedEntry.getTaskIdValue(),
                targetTask.getTaskCode(),
                targetTask.getName(),
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

        return new AdjustApprovedWorkLogResult(result, warnings);
    }

    private void checkBudgetWarning(Task task, List<String> warnings) {
        BigDecimal baseHours = task.getBudgetHours() != null && task.getBudgetHours().compareTo(BigDecimal.ZERO) > 0
                ? task.getBudgetHours()
                : task.getEstimatedHours();

        if (baseHours != null && baseHours.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actual = task.getActualHours() != null ? task.getActualHours() : BigDecimal.ZERO;
            BigDecimal threshold = baseHours.multiply(new BigDecimal("0.8"));

            if (actual.compareTo(threshold) >= 0) {
                BigDecimal percentage = actual.multiply(new BigDecimal("100"))
                        .divide(baseHours, 0, java.math.RoundingMode.HALF_UP);

                if (actual.compareTo(baseHours) > 0) {
                    warnings.add("Thời gian thực tế (" + actual + "h) đã VƯỢT quỹ thời gian ("
                            + baseHours + "h) - Đạt " + percentage + "%.");
                } else {
                    warnings.add("Thời gian thực tế (" + actual + "h) đã đạt "
                            + percentage + "% quỹ thời gian (" + baseHours + "h).");
                }
            }
        }
    }

    private String auditValue(String value) {
        return value == null ? "<null>" : "\"" + value + "\"";
    }
}

