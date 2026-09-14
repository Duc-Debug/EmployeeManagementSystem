package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.timesheet.UpdateWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
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
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class UpdateWorkLogServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
    @Mock
    private LoadTaskAssignmentPort loadTaskAssignmentPort;
    @Mock
    private LoadTimesheetPort loadTimesheetPort;
    @Mock
    private SaveTimesheetPort saveTimesheetPort;
    @Mock
    private LoadTimesheetEntryPort loadTimesheetEntryPort;
    @Mock
    private SaveTimesheetEntryPort saveTimesheetEntryPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private UpdateWorkLogService service;

    private final Long userId = 100L;
    private final EmployeeId employeeId = new EmployeeId(10L);
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId taskId = new TaskId(5L);
    private final LocalDate workDate = LocalDate.of(2026, 9, 14);

    @BeforeEach
    void setUp() {
        service = new UpdateWorkLogService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                saveAuditLogPort,
                authorizationService
        );
    }

    private Employee createMockEmployee(LocalDate contractEndDate) {
        return new Employee(
                employeeId,
                new UserId(userId),
                1L,
                "EMP-001",
                "Nguyễn Văn Chuyên Môn",
                "Kỹ sư",
                LocalDate.of(2024, 1, 1),
                contractEndDate,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private Project createMockProject(ProjectStatus status) {
        return new Project(
                projectId,
                "PROJ-001",
                "Hệ Thống Quản Lý Nhân Sự",
                1L,
                null,
                null,
                null,
                null,
                null,
                status,
                null,
                null,
                null,
                0L
        );
    }

    private Task createMockTask(TaskType type) {
        return new Task(
                taskId,
                projectId,
                null,
                "TASK-001",
                "Phát triển module chấm công",
                null,
                type,
                employeeId,
                BigDecimal.valueOf(40),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                TaskStatus.IN_PROGRESS,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0L
        );
    }

    @Test
    @DisplayName("Update Work Log: Chặn sửa work log sang ngày sau khi hợp đồng đã kết thúc (BLOCKER 2)")
    void testUpdateWorkLog_AfterContractEndDate_ThrowsException() {
        LocalDate contractEnd = LocalDate.of(2026, 9, 10);
        Employee emp = createMockEmployee(contractEnd);

        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(emp));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(emp));

        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(500L),
                new TimesheetId(200L),
                employeeId,
                projectId,
                taskId,
                LocalDate.of(2026, 9, 9),
                BigDecimal.valueOf(4.0),
                true,
                "Viết code",
                TimesheetStatus.DRAFT,
                null, null, 0L
        );
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(500L))).thenReturn(Optional.of(entry));

        Timesheet timesheet = new Timesheet(
                new TimesheetId(200L),
                employeeId,
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 14),
                BigDecimal.valueOf(4.0),
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of(entry)
        );
        when(loadTimesheetPort.findById(new TimesheetId(200L))).thenReturn(Optional.of(timesheet));

        // Attempting to move workDate to 2026-09-14 which is after contractEnd 2026-09-10
        UpdateWorkLogCommand command = new UpdateWorkLogCommand(
                500L,
                projectId.value(),
                taskId.value(),
                workDate, // 2026-09-14 > 2026-09-10
                BigDecimal.valueOf(4.0),
                true,
                "Viết code sau ngày hết hợp đồng"
        );

        assertThrows(WorkLogTaskNotAssignedException.class, () -> service.updateWorkLog(command));
    }

    @Test
    @DisplayName("Update Work Log: Chặn sửa entry không ở trạng thái DRAFT (HIGH 4)")
    void testUpdateWorkLog_NonDraftEntry_ThrowsException() {
        Employee emp = createMockEmployee(null);

        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(emp));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(emp));

        // Entry is APPROVED even if timesheet is DRAFT
        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(500L),
                new TimesheetId(200L),
                employeeId,
                projectId,
                taskId,
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Viết code",
                TimesheetStatus.APPROVED,
                null, null, 0L
        );
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(500L))).thenReturn(Optional.of(entry));

        Timesheet timesheet = new Timesheet(
                new TimesheetId(200L),
                employeeId,
                workDate,
                workDate.plusDays(6),
                BigDecimal.valueOf(4.0),
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of(entry)
        );
        when(loadTimesheetPort.findById(new TimesheetId(200L))).thenReturn(Optional.of(timesheet));

        UpdateWorkLogCommand command = new UpdateWorkLogCommand(
                500L,
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Sửa dòng đã duyệt"
        );

        assertThrows(TimesheetImmutableException.class, () -> service.updateWorkLog(command));
    }
}