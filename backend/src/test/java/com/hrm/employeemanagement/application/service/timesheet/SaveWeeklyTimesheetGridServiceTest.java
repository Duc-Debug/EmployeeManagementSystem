package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.timesheet.SaveWeeklyTimesheetGridCommand;
import com.hrm.employeemanagement.application.dto.timesheet.TaskDailyHourInputDto;
import com.hrm.employeemanagement.application.dto.timesheet.TaskWeeklyHoursInputDto;
import com.hrm.employeemanagement.application.dto.timesheet.WeeklyTimesheetResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetWeeklyTimesheetUseCase;
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
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetImmutableException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInClosedProjectException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class SaveWeeklyTimesheetGridServiceTest {

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
    private DeleteTimesheetEntryPort deleteTimesheetEntryPort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private GetWeeklyTimesheetUseCase getWeeklyTimesheetUseCase;
    @Mock
    private AuthorizationService authorizationService;

    private SaveWeeklyTimesheetGridService service;

    private Employee sampleEmployee;
    private Project sampleProject;
    private Task sampleTask;
    private LocalDate monday;

    private final EmployeeId employeeId = new EmployeeId(10L);
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId taskId = new TaskId(5L);

    @BeforeEach
    void setUp() {
        service = new SaveWeeklyTimesheetGridService(
                loadEmployeePort,
                loadProjectPort,
                loadTaskPort,
                loadTaskAssignmentPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                deleteTimesheetEntryPort,
                saveAuditLogPort,
                getWeeklyTimesheetUseCase,
                authorizationService
        );

        monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        sampleEmployee = new Employee(
                employeeId,
                new UserId(100L),
                1L,
                "EMP-001",
                "Nguyễn Văn Chuyên Môn",
                false,
                40,
                EmployeeStatus.ACTIVE
        );

        sampleProject = new Project(
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

        sampleTask = new Task(
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
    @DisplayName("NCL-09-CN-007-TC-01: Nhập lưới 3 task x các ngày trong tuần và lưu 1 lần thành công")
    void shouldSaveWeeklyGridSuccessfully() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(sampleEmployee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(sampleEmployee));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(sampleProject));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(sampleTask));

        Timesheet draftTs = Timesheet.create(employeeId, monday);
        draftTs.setId(new TimesheetId(50L));
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, monday)).thenReturn(Optional.of(draftTs));
        when(loadTimesheetEntryPort.findByEmployeeAndDateRange(employeeId, monday, monday.plusDays(6))).thenReturn(List.of());
        when(loadTimesheetEntryPort.findByTimesheetId(draftTs.getId())).thenReturn(List.of());

        WeeklyTimesheetResult mockResult = new WeeklyTimesheetResult(
                50L, 10L, "Nguyễn Văn Chuyên Môn", monday, monday.plusDays(6), BigDecimal.valueOf(12), "DRAFT", true, List.of(), List.of()
        );
        when(getWeeklyTimesheetUseCase.getMyWeeklyTimesheet(monday)).thenReturn(mockResult);

        TaskWeeklyHoursInputDto row = new TaskWeeklyHoursInputDto(
                1L, 5L, true, "Làm UI grid",
                List.of(
                        new TaskDailyHourInputDto(monday, BigDecimal.valueOf(4)),
                        new TaskDailyHourInputDto(monday.plusDays(1), BigDecimal.valueOf(4)),
                        new TaskDailyHourInputDto(monday.plusDays(2), BigDecimal.valueOf(4))
                )
        );

        SaveWeeklyTimesheetGridCommand cmd = new SaveWeeklyTimesheetGridCommand(monday, List.of(row));
        WeeklyTimesheetResult result = service.saveWeeklyGrid(cmd);

        assertNotNull(result);
        assertEquals(50L, result.timesheetId());
        verify(saveAuditLogPort).save(any());
        verify(saveTimesheetPort).save(any());
    }

    @Test
    @DisplayName("NCL-09-CN-007-TC-02: Tổng giờ làm trong 1 ngày vượt quá 12h (QTN-09) -> Báo lỗi chặn lưu")
    void shouldThrowDailyHoursLimitExceededWhenTotalExceeds12Hours() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(sampleEmployee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(sampleEmployee));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(sampleProject));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(sampleTask));

        Timesheet draftTs = Timesheet.create(employeeId, monday);
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, monday)).thenReturn(Optional.of(draftTs));
        when(loadTimesheetEntryPort.findByEmployeeAndDateRange(employeeId, monday, monday.plusDays(6))).thenReturn(List.of());

        TaskWeeklyHoursInputDto row = new TaskWeeklyHoursInputDto(
                1L, 5L, true, "Làm UI grid",
                List.of(
                        new TaskDailyHourInputDto(monday, BigDecimal.valueOf(14)) // 14 hours > 12h limit
                )
        );

        SaveWeeklyTimesheetGridCommand cmd = new SaveWeeklyTimesheetGridCommand(monday, List.of(row));
        assertThrows(DailyHoursLimitExceededException.class, () -> service.saveWeeklyGrid(cmd));
        verify(saveTimesheetEntryPort, never()).save(any());
    }

    @Test
    @DisplayName("QTN-08: Không ghi giờ vào dự án ở trạng thái CLOSED")
    void shouldThrowWorkLogInClosedProjectExceptionWhenProjectIsClosed() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(sampleEmployee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(sampleEmployee));

        Project closedProject = new Project(
                projectId, "PROJ-001", "Dự án đã đóng", 1L,
                null, null, null, null, null, ProjectStatus.CLOSED,
                null, null, null, 0L
        );
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(closedProject));

        Timesheet draftTs = Timesheet.create(employeeId, monday);
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, monday)).thenReturn(Optional.of(draftTs));

        TaskWeeklyHoursInputDto row = new TaskWeeklyHoursInputDto(
                1L, 5L, true, "Làm dự án đóng",
                List.of(new TaskDailyHourInputDto(monday, BigDecimal.valueOf(4)))
        );

        SaveWeeklyTimesheetGridCommand cmd = new SaveWeeklyTimesheetGridCommand(monday, List.of(row));
        assertThrows(WorkLogInClosedProjectException.class, () -> service.saveWeeklyGrid(cmd));
    }

    @Test
    @DisplayName("QTN-07: Bảng chấm công đã SUBMITTED hoặc APPROVED là bất biến -> Chặn lưu")
    void shouldThrowTimesheetImmutableExceptionWhenTimesheetIsNotDraft() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(sampleEmployee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(sampleEmployee));

        Timesheet submittedTs = new Timesheet(
                new TimesheetId(100L), employeeId, monday, monday.plusDays(6),
                BigDecimal.valueOf(40), TimesheetStatus.SUBMITTED, java.time.LocalDateTime.now(),
                null, null, null, java.time.LocalDateTime.now(), null, 0L, List.of()
        );
        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, monday)).thenReturn(Optional.of(submittedTs));

        TaskWeeklyHoursInputDto row = new TaskWeeklyHoursInputDto(
                1L, 5L, true, "Làm việc",
                List.of(new TaskDailyHourInputDto(monday, BigDecimal.valueOf(4)))
        );

        SaveWeeklyTimesheetGridCommand cmd = new SaveWeeklyTimesheetGridCommand(monday, List.of(row));
        assertThrows(TimesheetImmutableException.class, () -> service.saveWeeklyGrid(cmd));
    }

    @Test
    @DisplayName("QTN-09: Chặn lưu khi DB đã có 8h (Task A) và request mới gửi thêm 8h (Task B) làm tổng ngày = 16h > 12h")
    void shouldThrowDailyHoursLimitExceededWhenExistingEntryPlusNewTaskExceeds12Hours() {
        when(authorizationService.require(PermissionCode.WORK_LOG_CREATE)).thenReturn(100L);
        when(loadEmployeePort.findByUserId(new UserId(100L))).thenReturn(Optional.of(sampleEmployee));
        when(loadEmployeePort.findByIdForUpdate(employeeId)).thenReturn(Optional.of(sampleEmployee));
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(sampleProject));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(sampleTask));

        Timesheet draftTs = Timesheet.create(employeeId, monday);

        // Existing entry in DB for Task 99L on Monday = 8h
        com.hrm.employeemanagement.domain.timesheet.TimesheetEntry existingTaskAEntry =
                new com.hrm.employeemanagement.domain.timesheet.TimesheetEntry(
                        new com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId(999L),
                        draftTs.getId(),
                        employeeId,
                        projectId,
                        new TaskId(99L),
                        monday,
                        BigDecimal.valueOf(8),
                        true,
                        "Task A đã làm 8h",
                        TimesheetStatus.DRAFT,
                        java.time.LocalDateTime.now(),
                        null,
                        0L
                );

        when(loadTimesheetPort.findByEmployeeAndWeekStart(employeeId, monday)).thenReturn(Optional.of(draftTs));
        when(loadTimesheetEntryPort.findByEmployeeAndDateRange(employeeId, monday, monday.plusDays(6)))
                .thenReturn(List.of(existingTaskAEntry));

        // Payload only sends Task B (taskId = 5L) with 8h on Monday (Task A is omitted from request)
        TaskWeeklyHoursInputDto row = new TaskWeeklyHoursInputDto(
                1L, 5L, true, "Task B gửi thêm 8h",
                List.of(new TaskDailyHourInputDto(monday, BigDecimal.valueOf(8)))
        );

        SaveWeeklyTimesheetGridCommand cmd = new SaveWeeklyTimesheetGridCommand(monday, List.of(row));
        assertThrows(DailyHoursLimitExceededException.class, () -> service.saveWeeklyGrid(cmd));
        verify(saveTimesheetEntryPort, never()).save(any());
    }
}

