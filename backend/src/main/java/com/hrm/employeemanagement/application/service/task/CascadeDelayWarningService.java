package com.hrm.employeemanagement.application.service.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.task.cascade.CascadeDelayWarningResult;
import com.hrm.employeemanagement.application.dto.task.cascade.EvaluateCascadeDelayCommand;
import com.hrm.employeemanagement.application.port.inbound.task.EvaluateCascadeDelayUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskActualEndDateUseCase;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.cascade.CascadeDelayEvaluator;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;

public class CascadeDelayWarningService implements EvaluateCascadeDelayUseCase, UpdateTaskActualEndDateUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadTaskDependencyPort loadDependencyPort;
    private final LoadMilestonePort loadMilestonePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final AuthorizationService authorizationService;
    private final CascadeDelayEvaluator cascadeDelayEvaluator;

    public CascadeDelayWarningService(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskDependencyPort loadDependencyPort,
            LoadMilestonePort loadMilestonePort,
            SaveAuditLogPort saveAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort must not be null");
        this.loadDependencyPort = Objects.requireNonNull(loadDependencyPort, "LoadTaskDependencyPort must not be null");
        this.loadMilestonePort = Objects.requireNonNull(loadMilestonePort, "LoadMilestonePort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.cascadeDelayEvaluator = new CascadeDelayEvaluator();
    }

    @Override
    public CascadeDelayWarningResult evaluateCascadeDelay(EvaluateCascadeDelayCommand command) {
        validateCommand(command);

        // TC-03: Kiểm tra quyền truy cập cảnh báo trễ dây chuyền
        Long currentUserId = authorizationService.requireAny(
                PermissionCode.PROJECT_CASCADE_DELAY_READ,
                PermissionCode.PROJECT_CASCADE_DELAY_MANAGE
        );

        ProjectId projectId = new ProjectId(command.projectId());
        TaskId taskId = new TaskId(command.taskId());

        Project project = loadProjectPort.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        Task rootTask = loadTaskPort.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Không tìm thấy công việc với ID: " + command.taskId()));

        if (!rootTask.getProjectId().equals(projectId)) {
            throw new InvalidTaskDataException("Công việc không thuộc dự án chỉ định");
        }

        List<Task> allTasks = loadTaskPort.findAllByProjectId(projectId);
        List<TaskDependency> dependencies = loadDependencyPort.findByProjectId(projectId);
        List<Milestone> milestones = loadMilestonePort.findAllByProjectId(projectId);

        CascadeDelayWarningResult result = cascadeDelayEvaluator.evaluate(
                rootTask,
                command.newActualEndDate(),
                allTasks,
                dependencies,
                milestones
        );

        // TC-04: Lưu nhật ký xem/đánh giá ảnh hưởng trễ dây chuyền
        saveAuditLogPort.save(
                AuditLog.createChange(
                        currentUserId,
                        "CASCADE_DELAY_WARNING_EVALUATED",
                        "tasks",
                        taskId.value(),
                        null,
                        "newActualEndDate=" + command.newActualEndDate()
                                + ";slipDays=" + result.slipDays()
                                + ";affectedTasksCount=" + result.affectedTasks().size()
                                + ";chainOnTimeDueToSlack=" + result.chainOnTimeDueToSlack()
                )
        );

        return result;
    }

    @Override
    public CascadeDelayWarningResult updateActualEndDate(EvaluateCascadeDelayCommand command) {
        validateCommand(command);

        // TC-03: Kiểm tra quyền quản lý/cập nhật cảnh báo trễ dây chuyền
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_CASCADE_DELAY_MANAGE);

        ProjectId projectId = new ProjectId(command.projectId());
        TaskId taskId = new TaskId(command.taskId());

        Project project = loadProjectPort.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        Task rootTask = loadTaskPort.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Không tìm thấy công việc với ID: " + command.taskId()));

        if (!rootTask.getProjectId().equals(projectId)) {
            throw new InvalidTaskDataException("Công việc không thuộc dự án chỉ định");
        }

        List<Task> allTasks = loadTaskPort.findAllByProjectId(projectId);
        List<TaskDependency> dependencies = loadDependencyPort.findByProjectId(projectId);
        List<Milestone> milestones = loadMilestonePort.findAllByProjectId(projectId);

        CascadeDelayWarningResult result = cascadeDelayEvaluator.evaluate(
                rootTask,
                command.newActualEndDate(),
                allTasks,
                dependencies,
                milestones
        );

        LocalDate oldActualDate = rootTask.getActualEndDate();
        rootTask.updateActualEndDate(command.newActualEndDate());
        saveTaskPort.save(rootTask);

        // TC-04: Lưu lịch sử người thực hiện, nội dung và thời điểm xác nhận thao tác
        saveAuditLogPort.save(
                AuditLog.createChange(
                        currentUserId,
                        "TASK_ACTUAL_END_DATE_UPDATED",
                        "tasks",
                        taskId.value(),
                        oldActualDate != null ? oldActualDate.toString() : null,
                        command.newActualEndDate().toString()
                                + ";slipDays=" + result.slipDays()
                                + ";chainOnTimeDueToSlack=" + result.chainOnTimeDueToSlack()
                )
        );

        return result;
    }

    private void validateCommand(EvaluateCascadeDelayCommand command) {
        if (command == null) {
            throw new InvalidProjectDataException("Dữ liệu yêu cầu không được null");
        }
        if (command.projectId() == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }
        if (command.taskId() == null) {
            throw new InvalidTaskDataException("Mã công việc (taskId) không được để trống");
        }
        if (command.newActualEndDate() == null) {
            throw new InvalidTaskDataException("Ngày kết thúc thực tế mới không được để trống");
        }
    }
}
