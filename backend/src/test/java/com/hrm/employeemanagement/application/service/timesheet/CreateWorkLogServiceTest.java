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

import com.hrm.employeemanagement.application.dto.timesheet.CreateWorkLogCommand;
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
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogDescriptionBlankException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInClosedProjectException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogTaskNotAssignedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
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
class CreateWorkLogServiceTest {

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

    private CreateWorkLogService service;

    private final Long userId = 100L;
    private final EmployeeId employeeId = new EmployeeId(10L);
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId taskId = new TaskId(5L);
    private final LocalDate workDate = LocalDate.of(2026, 9, 14); // Monday

    @BeforeEach
    void setUp() {
        service = new CreateWorkLogService(
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
    @DisplayName("TC-01: Ghi 4 giờ thành công cho công việc được giao trong dự án đang chạy")
    void testCreateWorkLog_Success() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(createMockProject(ProjectStatus.ACTIVE)));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(createMockTask(TaskType.TASK)));
        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(employeeId, workDate, null)).thenReturn(BigDecimal.ZERO);

        Timesheet mockTimesheet = new Timesheet(
                new TimesheetId(200L),
                employeeId,
                workDate,
                workDate.plusDays(6),
                BigDecimal.ZERO,
                TimesheetStatus.DRAFT,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, workDate)).thenReturn(Optional.of(mockTimesheet));

        TimesheetEntry savedMockEntry = new TimesheetEntry(
                new TimesheetEntryId(300L),
                mockTimesheet.getId(),
                employeeId,
                projectId,
                taskId,
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Hoàn thành phân tích và thiết kế API",
                TimesheetStatus.DRAFT,
                null, null, 0L
        );
        when(saveTimesheetEntryPort.save(any(TimesheetEntry.class))).thenReturn(savedMockEntry);
        when(loadTimesheetEntryPort.findByTimesheetId(mockTimesheet.getId())).thenReturn(List.of(savedMockEntry));

        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Hoàn thành phân tích và thiết kế API"
        );

        WorkLogResult result = service.createWorkLog(command);

        assertNotNull(result);
        assertEquals(300L, result.id());
        assertEquals(BigDecimal.valueOf(4.0), result.hours());
        assertEquals("DRAFT", result.status());

        // Verify TC-05: Audit log recorded
        verify(saveAuditLogPort).save(argThat(log ->
                log.getUserId().equals(userId) &&
                "CREATE_WORK_LOG".equals(log.getAction()) &&
                "timesheet_entries".equals(log.getTableName())
        ));
    }

    @Test
    @DisplayName("TC-02: Báo lỗi khi số giờ là số âm hoặc lớn hơn 24")
    void testCreateWorkLog_InvalidHours() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));

        CreateWorkLogCommand negativeCommand = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(-2.0),
                true,
                "Mô tả hợp lệ"
        );

        assertThrows(WorkLogInvalidHoursException.class, () -> service.createWorkLog(negativeCommand));

        CreateWorkLogCommand over24Command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(25.0),
                true,
                "Mô tả hợp lệ"
        );

        assertThrows(WorkLogInvalidHoursException.class, () -> service.createWorkLog(over24Command));
    }

    @Test
    @DisplayName("TC-03: Báo lỗi khi để trống mô tả nội dung công việc")
    void testCreateWorkLog_BlankDescription() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));

        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "   "
        );

        assertThrows(WorkLogDescriptionBlankException.class, () -> service.createWorkLog(command));
    }

    @Test
    @DisplayName("TC-04: Từ chối khi nhân viên chưa được phân công công việc")
    void testCreateWorkLog_NotAssigned() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(createMockProject(ProjectStatus.ACTIVE)));

        // Task assigned to someone else
        Task otherTask = new Task(
                taskId,
                projectId,
                null,
                "TASK-002",
                "Công việc của người khác",
                null,
                TaskType.TASK,
                new EmployeeId(999L), // different employee
                BigDecimal.valueOf(20),
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
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(otherTask));
        when(loadTaskAssignmentPort.findByTaskIdAndEmployeeId(taskId, employeeId)).thenReturn(Optional.empty());

        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Thử ghi giờ cho việc không được giao"
        );

        assertThrows(WorkLogTaskNotAssignedException.class, () -> service.createWorkLog(command));
    }

    @Test
    @DisplayName("QTN-09: Chặn lưu khi tổng giờ làm trong ngày vượt quá 12 giờ")
    void testCreateWorkLog_ExceedsDailyLimit() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(createMockProject(ProjectStatus.ACTIVE)));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(createMockTask(TaskType.TASK)));
        // Already logged 9 hours on this day
        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(employeeId, workDate, null)).thenReturn(BigDecimal.valueOf(9.0));

        // Trying to log 4 more hours -> total 13 > 12
        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Ghi thêm 4 giờ"
        );

        assertThrows(DailyHoursLimitExceededException.class, () -> service.createWorkLog(command));
    }

    @Test
    @DisplayName("QTN-08: Chặn lưu khi dự án ở trạng thái đã đóng (CLOSED)")
    void testCreateWorkLog_ClosedProject() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(createMockProject(ProjectStatus.CLOSED)));

        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Ghi giờ cho dự án đã đóng"
        );

        assertThrows(WorkLogInClosedProjectException.class, () -> service.createWorkLog(command));
    }

    @Test
    @DisplayName("QTN-07: Chặn ghi giờ khi bảng chấm công tuần đã ở trạng thái SUBMITTED hoặc APPROVED")
    void testCreateWorkLog_SubmittedTimesheet() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(createMockEmployee()));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(createMockProject(ProjectStatus.ACTIVE)));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(createMockTask(TaskType.TASK)));
        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(employeeId, workDate, null)).thenReturn(BigDecimal.ZERO);

        Timesheet submittedTimesheet = new Timesheet(
                new TimesheetId(200L),
                employeeId,
                workDate,
                workDate.plusDays(6),
                BigDecimal.valueOf(40.0),
                TimesheetStatus.SUBMITTED,
                null, null, null, null, null, null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, workDate)).thenReturn(Optional.of(submittedTimesheet));

        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Ghi giờ vào bảng đã nộp"
        );

        assertThrows(TimesheetImmutableException.class, () -> service.createWorkLog(command));
    }

    @Test
    @DisplayName("Concurrency safety: Phải gọi findByIdForUpdate để serialize các thao tác ghi giờ chống race condition vỡ 12h/ngày")
    void testCreateWorkLog_AcquiresPessimisticLockForConcurrencySafety() {
        Employee employee = createMockEmployee();
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(userId);
        when(loadEmployeePort.findByUserId(new UserId(userId))).thenReturn(Optional.of(employee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(createMockProject(ProjectStatus.ACTIVE)));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(createMockTask(TaskType.TASK)));
        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(employeeId, workDate, null)).thenReturn(BigDecimal.valueOf(8.0));

        Timesheet draftTimesheet = Timesheet.create(employeeId, workDate);
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, workDate)).thenReturn(Optional.of(draftTimesheet));
        when(saveTimesheetEntryPort.save(any(TimesheetEntry.class))).thenAnswer(inv -> {
            TimesheetEntry e = inv.getArgument(0);
            return new TimesheetEntry(new TimesheetEntryId(999L), e.getTimesheetId(), e.getEmployeeId(), e.getProjectId(), e.getTaskId(), e.getWorkDate(), e.getHours(), e.isBillable(), e.getDescription(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt(), 0L);
        });

        CreateWorkLogCommand command = new CreateWorkLogCommand(
                projectId.value(),
                taskId.value(),
                workDate,
                BigDecimal.valueOf(4.0),
                true,
                "Ghi nốt 4h để đạt đúng 12h"
        );

        WorkLogResult result = service.createWorkLog(command);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(4.0), result.hours());
        verify(loadEmployeePort).findByIdForUpdate(employeeId);
    }
}
