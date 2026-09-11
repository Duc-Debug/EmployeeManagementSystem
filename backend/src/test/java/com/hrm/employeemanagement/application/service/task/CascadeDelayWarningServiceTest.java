package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.hrm.employeemanagement.application.dto.task.cascade.AffectedMilestoneResult;
import com.hrm.employeemanagement.application.dto.task.cascade.AffectedTaskResult;
import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.application.dto.task.cascade.EvaluateCascadeDelayCommand;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependencyType;
import com.hrm.employeemanagement.domain.user.UserId;

class CascadeDelayWarningServiceTest {

    @Mock
    private LoadProjectPort loadProjectPort;
    @Mock
    private LoadTaskPort loadTaskPort;
    @Mock
    private SaveTaskPort saveTaskPort;
    @Mock
    private LoadTaskDependencyPort loadDependencyPort;
    @Mock
    private LoadMilestonePort loadMilestonePort;
    @Mock
    private SaveAuditLogPort saveAuditLogPort;
    @Mock
    private AuthorizationService authorizationService;

    private CascadeDelayWarningService service;

    private final Long currentUserId = 100L;
    private final ProjectId projectId = new ProjectId(1L);
    private final TaskId task1Id = new TaskId(10L);
    private final TaskId task2Id = new TaskId(20L);
    private final MilestoneId milestoneId = new MilestoneId(30L);

    private Project testProject;
    private Task task1;
    private Task task2;
    private TaskDependency dependency;
    private Milestone milestone;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CascadeDelayWarningService(
                loadProjectPort,
                loadTaskPort,
                saveTaskPort,
                loadDependencyPort,
                loadMilestonePort,
                saveAuditLogPort,
                authorizationService
        );

        testProject = new Project(
                projectId,
                "PROJ-01",
                "Dự án thử nghiệm",
                10L,
                new EmployeeId(50L),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                BigDecimal.valueOf(100),
                "Mô tả dự án",
                com.hrm.employeemanagement.domain.project.ProjectStatus.ACTIVE,
                new UserId(currentUserId),
                null,
                null,
                0L
        );

        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate dueDate1 = LocalDate.of(2026, 10, 5);
        LocalDate dueDate2 = LocalDate.of(2026, 10, 15);

        task1 = new Task(
                task1Id,
                projectId,
                null,
                "TASK-01",
                "Công việc 1 (Đầu chuỗi)",
                "Mô tả 1",
                TaskType.TASK,
                new EmployeeId(50L),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                TaskStatus.IN_PROGRESS,
                1,
                startDate,
                dueDate1,
                null,
                0,
                new UserId(currentUserId),
                null,
                null,
                0L
        );

        task2 = new Task(
                task2Id,
                projectId,
                null,
                "TASK-02",
                "Công việc 2 (Phụ thuộc)",
                "Mô tả 2",
                TaskType.TASK,
                new EmployeeId(51L),
                BigDecimal.valueOf(10),
                BigDecimal.ZERO,
                BigDecimal.valueOf(10),
                TaskStatus.TODO,
                2,
                dueDate1.plusDays(1),
                dueDate2,
                null,
                0,
                new UserId(currentUserId),
                null,
                null,
                0L
        );

        dependency = new TaskDependency(
                1001L,
                projectId,
                task1Id,
                task2Id,
                TaskDependencyType.FINISH_TO_START,
                0,
                new UserId(currentUserId),
                null
        );

        milestone = new Milestone(
                milestoneId,
                projectId,
                "Mốc 1 (Phát hành Beta)",
                "Mô tả mốc",
                LocalDate.of(2026, 10, 20),
                null,
                Set.of(task2Id),
                new UserId(currentUserId),
                null,
                null,
                0L
        );
    }

    @Test
    @DisplayName("TC-01: Luồng thành công - Công việc đầu chuỗi trễ 3 ngày, hệ thống liệt kê công việc phía sau và mốc bị lùi 3 ngày")
    void testEvaluateCascadeDelay_TC01_SuccessFlow() {
        when(authorizationService.requireAny(
                PermissionCode.PROJECT_CASCADE_DELAY_READ,
                PermissionCode.PROJECT_CASCADE_DELAY_MANAGE
        )).thenReturn(currentUserId);

        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));
        when(loadTaskPort.findById(task1Id)).thenReturn(Optional.of(task1));
        when(loadTaskPort.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));
        when(loadDependencyPort.findByProjectId(projectId)).thenReturn(List.of(dependency));
        when(loadMilestonePort.findAllByProjectId(projectId)).thenReturn(List.of(milestone));

        LocalDate newActualEndDate = LocalDate.of(2026, 10, 8); // Slip by 3 days compared to due date (10-05)
        EvaluateCascadeDelayCommand command = new EvaluateCascadeDelayCommand(1L, 10L, newActualEndDate);

        CascadeDelayWarningResult result = service.evaluateCascadeDelay(command);

        assertNotNull(result);
        assertEquals(3L, result.slipDays());
        assertFalse(result.chainOnTimeDueToSlack());
        assertEquals(1, result.affectedTasks().size());

        AffectedTaskResult affectedTask = result.affectedTasks().get(0);
        assertEquals(20L, affectedTask.taskId());
        assertEquals(3L, affectedTask.delayDays());
        assertFalse(affectedTask.protectedBySlack());
        assertEquals(LocalDate.of(2026, 10, 18), affectedTask.newCalculatedEndDate());

        assertEquals(1, result.affectedMilestones().size());
        AffectedMilestoneResult affectedMilestone = result.affectedMilestones().get(0);
        assertEquals(30L, affectedMilestone.milestoneId());
        assertEquals(3L, affectedMilestone.delayDays());
        assertFalse(affectedMilestone.protectedBySlack());

        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-02: Ngoại lệ - Công việc trễ 3 ngày nhưng công việc phía sau có 5 ngày dự phòng, hệ thống báo chuỗi vẫn đúng hạn")
    void testEvaluateCascadeDelay_TC02_ProtectedBySlackTime() {
        when(authorizationService.requireAny(
                PermissionCode.PROJECT_CASCADE_DELAY_READ,
                PermissionCode.PROJECT_CASCADE_DELAY_MANAGE
        )).thenReturn(currentUserId);

        task2.setSlackDays(5); // 5 days slack time

        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));
        when(loadTaskPort.findById(task1Id)).thenReturn(Optional.of(task1));
        when(loadTaskPort.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));
        when(loadDependencyPort.findByProjectId(projectId)).thenReturn(List.of(dependency));
        when(loadMilestonePort.findAllByProjectId(projectId)).thenReturn(List.of(milestone));

        LocalDate newActualEndDate = LocalDate.of(2026, 10, 8); // Slip by 3 days
        EvaluateCascadeDelayCommand command = new EvaluateCascadeDelayCommand(1L, 10L, newActualEndDate);

        CascadeDelayWarningResult result = service.evaluateCascadeDelay(command);

        assertNotNull(result);
        assertEquals(3L, result.slipDays());
        assertTrue(result.chainOnTimeDueToSlack());
        assertEquals(1, result.affectedTasks().size());

        AffectedTaskResult affectedTask = result.affectedTasks().get(0);
        assertEquals(20L, affectedTask.taskId());
        assertEquals(0L, affectedTask.delayDays());
        assertTrue(affectedTask.protectedBySlack());
        assertTrue(affectedTask.statusDescription().contains("dự phòng"));

        verify(saveAuditLogPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("TC-03: Không có quyền - Người dùng từ chối truy cập và hệ thống ghi nhật ký từ chối")
    void testEvaluateCascadeDelay_TC03_NoPermission_ThrowsException() {
        when(authorizationService.requireAny(
                PermissionCode.PROJECT_CASCADE_DELAY_READ,
                PermissionCode.PROJECT_CASCADE_DELAY_MANAGE
        )).thenThrow(new PermissionDeniedException(PermissionCode.PROJECT_CASCADE_DELAY_READ));

        EvaluateCascadeDelayCommand command = new EvaluateCascadeDelayCommand(1L, 10L, LocalDate.of(2026, 10, 8));

        assertThrows(PermissionDeniedException.class, () -> service.evaluateCascadeDelay(command));
    }

    @Test
    @DisplayName("TC-04: Lưu lịch sử - Xác nhận cập nhật ngày kết thúc thực tế ghi nhật ký người thực hiện, nội dung và thời điểm")
    void testUpdateActualEndDate_TC04_SavesHistoryAndAuditLog() {
        when(authorizationService.require(PermissionCode.PROJECT_CASCADE_DELAY_MANAGE)).thenReturn(currentUserId);

        when(loadProjectPort.findById(projectId)).thenReturn(Optional.of(testProject));
        when(loadTaskPort.findById(task1Id)).thenReturn(Optional.of(task1));
        when(loadTaskPort.findAllByProjectId(projectId)).thenReturn(List.of(task1, task2));
        when(loadDependencyPort.findByProjectId(projectId)).thenReturn(List.of(dependency));
        when(loadMilestonePort.findAllByProjectId(projectId)).thenReturn(List.of(milestone));

        LocalDate newActualEndDate = LocalDate.of(2026, 10, 8);
        EvaluateCascadeDelayCommand command = new EvaluateCascadeDelayCommand(1L, 10L, newActualEndDate);

        CascadeDelayWarningResult result = service.updateActualEndDate(command);

        assertNotNull(result);
        assertEquals(newActualEndDate, task1.getActualEndDate());

        verify(saveTaskPort).save(task1);
        verify(saveAuditLogPort).save(any(AuditLog.class));
    }
}
