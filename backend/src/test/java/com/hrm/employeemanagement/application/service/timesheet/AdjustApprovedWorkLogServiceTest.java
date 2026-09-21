package com.hrm.employeemanagement.application.service.timesheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogCommand;
import com.hrm.employeemanagement.application.dto.timesheet.AdjustApprovedWorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.period.CheckAllocationPeriodLockUseCase;
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
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.timesheet.DailyHoursLimitExceededException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetEntryVersionConflictException;
import com.hrm.employeemanagement.domain.exception.timesheet.TimesheetNotApprovedException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogAdjustmentReasonRequiredException;
import com.hrm.employeemanagement.domain.exception.timesheet.WorkLogInvalidHoursException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.timesheet.Timesheet;
import com.hrm.employeemanagement.domain.timesheet.TimesheetAuditLog;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntryId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class AdjustApprovedWorkLogServiceTest {

    @Mock
    private LoadTimesheetEntryPort loadTimesheetEntryPort;
    @Mock
    private SaveTimesheetEntryPort saveTimesheetEntryPort;
    @Mock
    private LoadTimesheetPort loadTimesheetPort;
    @Mock
    private SaveTimesheetPort saveTimesheetPort;
    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
    @Mock
    private SaveTaskPort saveTaskPort;
    @Mock
    private LoadEmployeePort loadEmployeePort;
    @Mock
    private SaveTimesheetAuditLogPort saveTimesheetAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private CheckAllocationPeriodLockUseCase checkAllocationPeriodLockUseCase;

    private AdjustApprovedWorkLogService service;

    private final Long pmUserId = 100L;
    private final EmployeeId pmEmployeeId = new EmployeeId(10L);
    private final EmployeeId devEmployeeId = new EmployeeId(20L);
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId taskId = new TaskId(5L);
    private final LocalDate workDate = LocalDate.of(2026, 9, 14);

    @BeforeEach
    void setUp() {
        service = new AdjustApprovedWorkLogService(
                loadTimesheetEntryPort,
                saveTimesheetEntryPort,
                loadTimesheetPort,
                saveTimesheetPort,
                loadProjectPort,
                loadTaskPort,
                saveTaskPort,
                loadEmployeePort,
                saveTimesheetAuditLogPort,
                authorizationService,
                checkAllocationPeriodLockUseCase
        );
        // Successful adjustments must acquire the aggregate lock before checking 12h/day.
        lenient().when(loadEmployeePort.findByIdForUpdate(devEmployeeId))
                .thenReturn(Optional.of(createEmployee(devEmployeeId, "Lê Văn Dev")));
    }

    private Employee createEmployee(EmployeeId empId, String fullName) {
        return new Employee(
                empId,
                new UserId(empId.value() + 50),
                1L,
                "EMP-00" + empId.value(),
                fullName,
                "Kỹ sư",
                LocalDate.of(2024, 1, 1),
                null,
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private Project createProject(ProjectId pId, EmployeeId managerId) {
        return new Project(
                pId,
                "PROJ-01",
                "Dự Án Alpha",
                1L,
                managerId,
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

    private Task createTask(TaskId tId, ProjectId pId, BigDecimal estimated, BigDecimal actual) {
        return new Task(
                tId,
                pId,
                null,
                "TASK-01",
                "Phát triển API",
                null,
                TaskType.TASK,
                null,
                estimated,
                actual,
                BigDecimal.ZERO,
                com.hrm.employeemanagement.domain.task.TaskStatus.IN_PROGRESS,
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

    private TimesheetEntry createApprovedEntry(TimesheetEntryId id, TimesheetId tsId, BigDecimal hours) {
        return new TimesheetEntry(
                id,
                tsId,
                devEmployeeId,
                projectId,
                taskId,
                workDate,
                hours,
                true,
                "Làm việc bình thường",
                TimesheetStatus.APPROVED,
                null,
                null,
                0L
        );
    }

    @Test
    @DisplayName("TC-01: Điều chỉnh giờ đã duyệt thành công bởi PM phụ trách dự án")
    void testAdjustApproved_Success() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        TimesheetEntry entry = createApprovedEntry(new TimesheetEntryId(1L), new TimesheetId(10L), new BigDecimal("8.00"));
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        Project project = createProject(projectId, pmEmployeeId);
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(project));

        Task task = createTask(taskId, projectId, new BigDecimal("40.00"), new BigDecimal("8.00"));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(task));

        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(devEmployeeId, workDate, entry.getId()))
                .thenReturn(BigDecimal.ZERO);

        when(saveTimesheetEntryPort.save(any(TimesheetEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Timesheet timesheet = Timesheet.create(devEmployeeId, workDate);
        timesheet.setId(new TimesheetId(10L));
        when(loadTimesheetPort.findById(new TimesheetId(10L))).thenReturn(Optional.of(timesheet));
        when(loadTimesheetEntryPort.findByTimesheetId(new TimesheetId(10L))).thenReturn(List.of(entry));

        Employee dev = createEmployee(devEmployeeId, "Lê Văn Dev");
        when(loadEmployeePort.findById(devEmployeeId)).thenReturn(Optional.of(dev));

        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("6.00"),
                null,
                true,
                "Điều chỉnh lại số giờ làm việc thực tế",
                "Bù giờ công tác theo yêu cầu thực tế",
                0L
        );

        AdjustApprovedWorkLogResult result = service.adjustApprovedWorkLog(command);

        assertNotNull(result);
        assertEquals(new BigDecimal("6.00"), result.entry().hours());
        assertEquals("APPROVED", result.entry().status());
        verify(loadEmployeePort).findByIdForUpdate(devEmployeeId);
        verify(saveTaskPort).save(task);
        assertEquals(new BigDecimal("6.00"), task.getActualHours());
        verify(saveTimesheetPort).save(any(Timesheet.class));

        ArgumentCaptor<TimesheetAuditLog> auditCaptor = ArgumentCaptor.forClass(TimesheetAuditLog.class);
        verify(saveTimesheetAuditLogPort).save(auditCaptor.capture());
        String auditNote = auditCaptor.getValue().getNote();
        assertTrue(auditNote.contains("Giờ: 8.00h -> 6.00h"));
        assertTrue(auditNote.contains("Công việc: TASK-01 -> TASK-01"));
        assertTrue(auditNote.contains("Tính phí: true -> true"));
        assertTrue(auditNote.contains("Mô tả: \"Làm việc bình thường\" -> \"Điều chỉnh lại số giờ làm việc thực tế\""));
    }

    @Test
    @DisplayName("TC-03: Thất bại khi lý do giải trình bị rỗng hoặc dưới 10 ký tự")
    void testAdjustApproved_ThrowsException_WhenReasonTooShort() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("6.00"),
                null,
                true,
                "Mô tả",
                "Ngắn", // dưới 10 ký tự
                0L
        );

        assertThrows(WorkLogAdjustmentReasonRequiredException.class, () -> service.adjustApprovedWorkLog(command));
    }

    @Test
    @DisplayName("TC-04: Thất bại khi dòng ghi giờ không ở trạng thái APPROVED")
    void testAdjustApproved_ThrowsException_WhenStatusNotApproved() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(1L),
                new TimesheetId(10L),
                devEmployeeId,
                projectId,
                taskId,
                workDate,
                new BigDecimal("8.00"),
                true,
                "Đang ở trạng thái SUBMITTED",
                TimesheetStatus.SUBMITTED,
                null,
                null,
                0L
        );
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("6.00"),
                null,
                true,
                "Mô tả hợp lệ",
                "Lý do giải trình hợp lệ trên 10 ký tự",
                0L
        );

        assertThrows(TimesheetNotApprovedException.class, () -> service.adjustApprovedWorkLog(command));
    }

    @Test
    @DisplayName("TC-02: Thất bại khi số giờ mới khiến tổng giờ trong ngày vượt 12h (QTN-09)")
    void testAdjustApproved_ThrowsException_WhenExceeds12Hours() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        TimesheetEntry entry = createApprovedEntry(new TimesheetEntryId(1L), new TimesheetId(10L), new BigDecimal("4.00"));
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        Project project = createProject(projectId, pmEmployeeId);
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(project));

        Task task = createTask(taskId, projectId, new BigDecimal("40.00"), new BigDecimal("4.00"));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(task));

        // Nhân viên đã có 9h ở các task khác trong ngày
        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(devEmployeeId, workDate, entry.getId()))
                .thenReturn(new BigDecimal("9.00"));

        // Điều chỉnh tăng thành 5h -> tổng là 14h > 12h
        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("5.00"),
                null,
                true,
                "Mô tả hợp lệ",
                "Lý do giải trình chi tiết hợp lệ",
                0L
        );

        assertThrows(DailyHoursLimitExceededException.class, () -> service.adjustApprovedWorkLog(command));
    }

    @Test
    @DisplayName("TC-05: Thất bại khi người dùng không phải PM dự án và không có quyền WORK_LOG_ADJUST")
    void testAdjustApproved_ThrowsException_WhenNotProjectManager() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee otherEmployee = createEmployee(new EmployeeId(99L), "Người lạ");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(otherEmployee));

        TimesheetEntry entry = createApprovedEntry(new TimesheetEntryId(1L), new TimesheetId(10L), new BigDecimal("8.00"));
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        // Dự án do pmEmployeeId quản lý, không phải otherEmployee
        Project project = createProject(projectId, pmEmployeeId);
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(project));
        when(authorizationService.hasPermission(PermissionCode.WORK_LOG_ADJUST)).thenReturn(false);

        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("6.00"),
                null,
                true,
                "Mô tả hợp lệ",
                "Lý do giải trình chi tiết hợp lệ",
                0L
        );

        assertThrows(PermissionDeniedException.class, () -> service.adjustApprovedWorkLog(command));
    }

    @Test
    @DisplayName("TC-06: Cảnh báo ngân sách Task khi điều chỉnh làm tăng tổng giờ thực tế vượt 80%")
    void testAdjustApproved_ReturnsBudgetWarning_WhenExceedsThreshold() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        TimesheetEntry entry = createApprovedEntry(new TimesheetEntryId(1L), new TimesheetId(10L), new BigDecimal("5.00"));
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        Project project = createProject(projectId, pmEmployeeId);
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(project));

        // Ngân sách 10h, hiện tại actual là 7h. Tăng từ 5h lên 7h -> projectedTotal = 7 - 5 + 7 = 9h (90% >= 80%)
        Task task = createTask(taskId, projectId, new BigDecimal("10.00"), new BigDecimal("7.00"));
        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(task));

        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(devEmployeeId, workDate, entry.getId()))
                .thenReturn(BigDecimal.ZERO);

        when(saveTimesheetEntryPort.save(any(TimesheetEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Timesheet timesheet = Timesheet.create(devEmployeeId, workDate);
        timesheet.setId(new TimesheetId(10L));
        when(loadTimesheetPort.findById(new TimesheetId(10L))).thenReturn(Optional.of(timesheet));
        when(loadTimesheetEntryPort.findByTimesheetId(new TimesheetId(10L))).thenReturn(List.of(entry));

        Employee dev = createEmployee(devEmployeeId, "Lê Văn Dev");
        when(loadEmployeePort.findById(devEmployeeId)).thenReturn(Optional.of(dev));

        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("7.00"),
                null,
                true,
                "Điều chỉnh thêm giờ",
                "Lý do giải trình hợp lệ cho việc tăng giờ",
                0L
        );

        AdjustApprovedWorkLogResult result = service.adjustApprovedWorkLog(command);

        assertNotNull(result);
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().get(0).contains("đạt 90% quỹ thời gian"));
        verify(saveTaskPort).save(task);
        assertEquals(new BigDecimal("9.00"), task.getActualHours());
    }

    @Test
    @DisplayName("TC-07: Điều chỉnh chuyển Task (từ Task A sang Task B) - đồng bộ đúng actualHours của cả 2 task và cảnh báo ngân sách Task B")
    void testAdjustApproved_TaskMove() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        TimesheetEntry entry = createApprovedEntry(new TimesheetEntryId(1L), new TimesheetId(10L), new BigDecimal("8.00"));
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        Project project = createProject(projectId, pmEmployeeId);
        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(project));

        TaskId taskBId = new TaskId(6L);
        Task taskA = createTask(taskId, projectId, new BigDecimal("40.00"), new BigDecimal("20.00"));
        Task taskB = new Task(
                taskBId,
                projectId,
                null,
                "TASK-02",
                "Viết Unit Test",
                null,
                TaskType.TASK,
                null,
                new BigDecimal("18.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                com.hrm.employeemanagement.domain.task.TaskStatus.IN_PROGRESS,
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

        when(loadTaskPort.findById(taskId)).thenReturn(Optional.of(taskA));
        when(loadTaskPort.findById(taskBId)).thenReturn(Optional.of(taskB));

        when(loadTimesheetEntryPort.sumHoursByEmployeeAndDate(devEmployeeId, workDate, entry.getId()))
                .thenReturn(BigDecimal.ZERO);

        when(saveTimesheetEntryPort.save(any(TimesheetEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Timesheet timesheet = Timesheet.create(devEmployeeId, workDate);
        timesheet.setId(new TimesheetId(10L));
        when(loadTimesheetPort.findById(new TimesheetId(10L))).thenReturn(Optional.of(timesheet));
        when(loadTimesheetEntryPort.findByTimesheetId(new TimesheetId(10L))).thenReturn(List.of(entry));

        Employee dev = createEmployee(devEmployeeId, "Lê Văn Dev");
        when(loadEmployeePort.findById(devEmployeeId)).thenReturn(Optional.of(dev));

        // Chuyển từ Task A (8h) sang Task B thành 6h
        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("6.00"),
                6L,
                true,
                "Chuyển nhầm công việc, ghi nhận lại sang Task B",
                "Lý do giải trình chi tiết về việc chuyển công việc",
                0L
        );

        AdjustApprovedWorkLogResult result = service.adjustApprovedWorkLog(command);

        assertNotNull(result);
        assertEquals(6L, result.entry().taskId());
        assertEquals("TASK-02", result.entry().taskCode());
        assertEquals(new BigDecimal("6.00"), result.entry().hours());

        // Task A phải giảm 8h (20 - 8 = 12h)
        assertEquals(new BigDecimal("12.00"), taskA.getActualHours());
        verify(saveTaskPort).save(taskA);

        // Task B phải tăng 6h (10 + 6 = 16h)
        assertEquals(new BigDecimal("16.00"), taskB.getActualHours());
        verify(saveTaskPort).save(taskB);

        // Cảnh báo ngân sách cho Task B: 16h / 18h = 89% (>= 80%)
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().get(0).contains("đạt 89% quỹ thời gian (18.00h)"));
    }

    @Test
    @DisplayName("TC-08: Thất bại khi xung đột phiên bản (Optimistic Locking - version mismatch)")
    void testAdjustApproved_ThrowsException_WhenVersionConflict() {
        when(authorizationService.requireAny(PermissionCode.WORK_LOG_ADJUST, PermissionCode.WORK_LOG_APPROVE))
                .thenReturn(pmUserId);

        Employee pm = createEmployee(pmEmployeeId, "PM Nguyễn Văn A");
        when(loadEmployeePort.findByUserId(new UserId(pmUserId))).thenReturn(Optional.of(pm));

        // Entry trong DB đang có version = 1
        TimesheetEntry entry = new TimesheetEntry(
                new TimesheetEntryId(1L),
                new TimesheetId(10L),
                devEmployeeId,
                projectId,
                taskId,
                workDate,
                new BigDecimal("8.00"),
                true,
                "Làm việc bình thường",
                TimesheetStatus.APPROVED,
                null,
                null,
                1L
        );
        when(loadTimesheetEntryPort.findById(new TimesheetEntryId(1L))).thenReturn(Optional.of(entry));

        // Request gửi lên version = 0 (stale)
        AdjustApprovedWorkLogCommand command = new AdjustApprovedWorkLogCommand(
                1L,
                new BigDecimal("6.00"),
                null,
                true,
                "Mô tả",
                "Lý do giải trình hợp lệ trên 10 ký tự",
                0L
        );

        assertThrows(TimesheetEntryVersionConflictException.class, () -> service.adjustApprovedWorkLog(command));
    }
}
