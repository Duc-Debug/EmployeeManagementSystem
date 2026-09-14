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
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.timesheet.SubmitWeeklyTimesheetCommand;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
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
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.EmptyTimesheetSubmissionException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException;
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
class SubmitWeeklyTimesheetServiceTest {

    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
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

    private SubmitWeeklyTimesheetService service;

    private final Long userId = 100L;
    private final EmployeeId employeeId = new EmployeeId(10L);
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId taskId = new TaskId(5L);
    private final LocalDate weekStartDate = LocalDate.of(2026, 9, 14); // Monday

    @BeforeEach
    void setUp() {
        service = new SubmitWeeklyTimesheetService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                saveAuditLogPort,
                authorizationService
        );
    }

    private Employee createMockEmployee() {
        return new Employee(
                employeeId,
                new UserId(userId),
                1L,
                "EMP-001",
                "Nguyễn Văn Chuyên Môn",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private Project createMockProject() {
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
                ProjectStatus.ACTIVE,
                null,
                null,
                null,
                0L
        );
    }

    private Task createMockTask() {
        return new Task(
                taskId,
                projectId,
                null,
                "TASK-001",
                "Phát triển module chấm công",
                null,
                TaskType.TASK,
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
    @DisplayName("TC-01: Nộp bảng chấm công tuần thành công khi đã ghi đủ giờ công hợp lệ")
    void testSubmitWeeklyTimesheet_Success() {
        Employee employee = createMockEmployee();
        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        Timesheet timesheet = new Timesheet(
                new TimesheetId(100L),
                employeeId,
                weekStartDate,
                weekStartDate.plusDays(6),
                BigDecimal.valueOf(8.0),
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, weekStartDate)).thenReturn(Optional.of(timesheet));

        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(200L),
                timesheet.getId(),
                employeeId,
                projectId,
                taskId,
                weekStartDate,
                BigDecimal.valueOf(8.0),
                true,
                "Phát triển tính năng nộp bảng chấm công",
                TimesheetStatus.DRAFT,
                null, null, 0L
        );
        when(loadTimesheetEntryPort.findByTimesheetId(timesheet.getId())).thenReturn(List.of(entry));
        when(saveTimesheetPort.save(any(TimesheetEntry.class.isInstance(timesheet) ? Timesheet.class : Timesheet.class))).thenAnswer(inv -> inv.getArgument(0));

        when(loadProjectPort.findAllById(List.of(projectId))).thenReturn(List.of(createMockProject()));
        when(loadTaskPort.findAllById(List.of(taskId))).thenReturn(List.of(createMockTask()));

        SubmitWeeklyTimesheetCommand command = new SubmitWeeklyTimesheetCommand(weekStartDate);
        WeeklyTimesheetResult result = service.submitWeeklyTimesheet(command);

        assertNotNull(result);
        assertEquals("SUBMITTED", result.status());
        assertEquals(0, result.totalHours().compareTo(BigDecimal.valueOf(8.0)));
        assertEquals(false, result.isEditable());

        // Verify TC-05: Audit log saved
        verify(saveAuditLogPort).save(argThat(log ->
                log.getUserId().equals(userId) &&
                "SUBMIT_TIMESHEET".equals(log.getAction()) &&
                "timesheets".equals(log.getTableName()) &&
                log.getRecordId().equals(100L)
        ));
    }

    @Test
    @DisplayName("TC-02: Chặn nộp bảng chấm công khi có một ngày vượt quá 12 giờ (QTN-09)")
    void testSubmitWeeklyTimesheet_ExceedsDailyLimit_ThrowsException() {
        Employee employee = createMockEmployee();
        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        Timesheet timesheet = new Timesheet(
                new TimesheetId(100L),
                employeeId,
                weekStartDate,
                weekStartDate.plusDays(6),
                BigDecimal.valueOf(14.0),
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, weekStartDate)).thenReturn(Optional.of(timesheet));

        // 2 entries on the same day totaling 14 hours > 12h
        TimesheetEntry entry1 = new TimesheetEntry(
                new TimesheetEntryId(201L),
                timesheet.getId(),
                employeeId,
                projectId,
                taskId,
                weekStartDate,
                BigDecimal.valueOf(8.0),
                true,
                "Task 1",
                TimesheetStatus.DRAFT,
                null, null, 0L
        );
        TimesheetEntry entry2 = new TimesheetEntry(
                new TimesheetEntryId(202L),
                timesheet.getId(),
                employeeId,
                projectId,
                taskId,
                weekStartDate,
                BigDecimal.valueOf(6.0),
                true,
                "Task 2",
                TimesheetStatus.DRAFT,
                null, null, 0L
        );
        when(loadTimesheetEntryPort.findByTimesheetId(timesheet.getId())).thenReturn(List.of(entry1, entry2));

        SubmitWeeklyTimesheetCommand command = new SubmitWeeklyTimesheetCommand(weekStartDate);
        assertThrows(DailyHoursLimitExceededException.class, () -> service.submitWeeklyTimesheet(command));
    }

    @Test
    @DisplayName("Ngoại lệ: Chặn nộp khi bảng chấm công rỗng (không có dòng ghi giờ)")
    void testSubmitWeeklyTimesheet_EmptyTimesheet_ThrowsException() {
        Employee employee = createMockEmployee();
        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        Timesheet timesheet = new Timesheet(
                new TimesheetId(100L),
                employeeId,
                weekStartDate,
                weekStartDate.plusDays(6),
                BigDecimal.ZERO,
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, weekStartDate)).thenReturn(Optional.of(timesheet));
        when(loadTimesheetEntryPort.findByTimesheetId(timesheet.getId())).thenReturn(List.of());

        SubmitWeeklyTimesheetCommand command = new SubmitWeeklyTimesheetCommand(weekStartDate);
        assertThrows(EmptyTimesheetSubmissionException.class, () -> service.submitWeeklyTimesheet(command));
    }

    @Test
    @DisplayName("TC-03: Chặn nộp khi bảng chấm công đã ở trạng thái SUBMITTED hoặc APPROVED")
    void testSubmitWeeklyTimesheet_AlreadySubmitted_ThrowsException() {
        Employee employee = createMockEmployee();
        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        Timesheet timesheet = new Timesheet(
                new TimesheetId(100L),
                employeeId,
                weekStartDate,
                weekStartDate.plusDays(6),
                BigDecimal.valueOf(8.0),
                TimesheetStatus.SUBMITTED,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, weekStartDate)).thenReturn(Optional.of(timesheet));

        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(200L),
                timesheet.getId(),
                employeeId,
                projectId,
                taskId,
                weekStartDate,
                BigDecimal.valueOf(8.0),
                true,
                "Task 1",
                TimesheetStatus.SUBMITTED,
                null, null, 0L
        );
        when(loadTimesheetEntryPort.findByTimesheetId(timesheet.getId())).thenReturn(List.of(entry));

        SubmitWeeklyTimesheetCommand command = new SubmitWeeklyTimesheetCommand(weekStartDate);
        assertThrows(TimesheetImmutableException.class, () -> service.submitWeeklyTimesheet(command));
    }

    @Test
    @DisplayName("Ngoại lệ: Chặn nộp khi nhân sự không ở trạng thái ACTIVE hoặc null")
    void testSubmitWeeklyTimesheet_InactiveEmployee_ThrowsException() {
        Employee inactiveEmployee = new Employee(
                employeeId,
                new UserId(userId),
                1L,
                "EMP-001",
                "Nguyễn Văn Chuyên Môn",
                false,
                40,
                EmployeeStatus.TERMINATED
        );
        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(inactiveEmployee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(inactiveEmployee));

        SubmitWeeklyTimesheetCommand command = new SubmitWeeklyTimesheetCommand(weekStartDate);
        assertThrows(com.hrm.employeemanagement.domain.exception.employee.EmployeeInactiveException.class,
                () -> service.submitWeeklyTimesheet(command));
    }

    @Test
    @DisplayName("Đảm bảo weekMonday/weekSunday luôn lấy từ timesheet khi truyền timesheetId")
    void testSubmitWeeklyTimesheet_WithTimesheetId_UsesTimesheetWeekRange() {
        Employee employee = createMockEmployee();
        when(authorizationService.require(PermissionCode.WORK_LOG_UPDATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        LocalDate actualWeekStart = LocalDate.of(2026, 9, 7); // Previous week
        Timesheet timesheet = new Timesheet(
                new TimesheetId(105L),
                employeeId,
                actualWeekStart,
                actualWeekStart.plusDays(6),
                BigDecimal.valueOf(8.0),
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findById(new TimesheetId(105L))).thenReturn(Optional.of(timesheet));

        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(205L),
                timesheet.getId(),
                employeeId,
                projectId,
                taskId,
                actualWeekStart,
                BigDecimal.valueOf(8.0),
                true,
                "Task 1",
                TimesheetStatus.DRAFT,
                null, null, 0L
        );
        when(loadTimesheetEntryPort.findByTimesheetId(timesheet.getId())).thenReturn(List.of(entry));
        when(saveTimesheetPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loadProjectPort.findAllById(any())).thenReturn(List.of(createMockProject()));
        when(loadTaskPort.findAllById(any())).thenReturn(List.of(createMockTask()));

        // Command passes dateInWeek as 2026-09-14, but timesheetId 105 belongs to 2026-09-07
        SubmitWeeklyTimesheetCommand command = new SubmitWeeklyTimesheetCommand(weekStartDate, 105L);
        WeeklyTimesheetResult result = service.submitWeeklyTimesheet(command);

        assertNotNull(result);
        assertEquals(actualWeekStart, result.weekStartDate());
        assertEquals(actualWeekStart.plusDays(6), result.weekEndDate());
        assertEquals("SUBMITTED", result.status());
    }
}