package com.hrm.employeemanagement.application.service.task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.task.MoveTaskBoardStatusCommand;
import com.hrm.employeemanagement.application.dto.task.TaskBoardAssigneeResult;
import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;
import com.hrm.employeemanagement.application.port.inbound.task.MoveTaskBoardStatusUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotAssignedToUserException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.user.User;

public class MoveTaskBoardStatusService implements MoveTaskBoardStatusUseCase {

    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    public MoveTaskBoardStatusService(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            GetAuthenticatedUserPort authenticatedUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort) {
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "GetAuthenticatedUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public TaskBoardCardResult moveTaskStatus(MoveTaskBoardStatusCommand command) {
        if (command == null || command.taskId() == null || command.newStatus() == null) {
            throw new InvalidTaskDataException("Mã công việc (taskId) và trạng thái mới (newStatus) không được để trống");
        }

        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Task task = loadTaskPort.findById(new TaskId(command.taskId()))
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        Project project = loadProjectPort.findById(task.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + task.getProjectIdValue()));

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        Employee currentEmployee = loadEmployeePort.findByUserId(currentUser.getId()).orElse(null);
        List<TaskAssignment> assignments = loadTaskAssignmentPort.findByTaskId(task.getId());

        boolean isAssigned = currentEmployee != null && assignments.stream()
                .anyMatch(a -> Objects.equals(a.getEmployeeId(), currentEmployee.getId()));

        boolean isProjectManager = currentEmployee != null && project.isManagedBy(currentEmployee.getId());

        boolean isCompanyScope = currentUser.getDataScope() == DataScope.COMPANY;

        // TC-02: Thẻ thuộc công việc của người khác -> Chặn và giữ nguyên trạng thái
        if (!isAssigned && !isProjectManager && !isCompanyScope) {
            saveDeniedAuditLogPort.save(AuditLog.createChange(
                    currentUser.getIdValue(),
                    "TASK_ACCESS_DENIED",
                    "tasks",
                    task.getIdValue(),
                    task.getStatus() != null ? task.getStatus().name() : null,
                    "reason=NOT_ASSIGNED_TO_USER;targetStatus=" + command.newStatus().name()
            ));
            throw new TaskNotAssignedToUserException(task.getIdValue(), currentEmployee != null ? currentEmployee.getIdValue() : null);
        }

        TaskStatus oldStatus = task.getStatus();

        // Kéo thả thẻ vào cùng một cột -> No-op
        if (oldStatus == command.newStatus()) {
            return buildCardResult(task, project, assignments);
        }

        // TC-01: Cập nhật trạng thái công việc
        task.updateStatus(command.newStatus());
        Task savedTask = saveTaskPort.save(task);

        // TC-03: Lưu nhật ký kiểm toán thay đổi trạng thái bảng công việc
        saveAuditLogPort.save(AuditLog.createChange(
                currentUser.getIdValue(),
                "MOVE_TASK_BOARD_STATUS",
                "tasks",
                savedTask.getIdValue(),
                oldStatus != null ? oldStatus.name() : null,
                command.newStatus().name()
        ));

        return buildCardResult(savedTask, project, assignments);
    }

    private TaskBoardCardResult buildCardResult(Task task, Project project, List<TaskAssignment> assignments) {
        List<EmployeeId> empIds = assignments.stream()
                .map(TaskAssignment::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Employee> employees = empIds.isEmpty() ? Collections.emptyList() : loadEmployeePort.findAllByIdIn(empIds);

        List<TaskBoardAssigneeResult> assigneeResults = assignments.stream()
                .map(assignment -> {
                    Employee emp = employees.stream()
                            .filter(e -> Objects.equals(e.getId(), assignment.getEmployeeId()))
                            .findFirst()
                            .orElse(null);
                    return new TaskBoardAssigneeResult(
                            assignment.getEmployeeId() != null ? assignment.getEmployeeId().value() : null,
                            emp != null ? emp.getEmployeeCode() : null,
                            emp != null ? emp.getFullName() : null,
                            assignment.isPrimary()
                    );
                })
                .sorted(Comparator.comparing(TaskBoardAssigneeResult::isPrimary).reversed())
                .toList();

        return new TaskBoardCardResult(
                task.getIdValue(),
                task.getTaskCode(),
                task.getName(),
                task.getDescription(),
                task.getProjectIdValue(),
                project != null ? project.getProjectCode() : null,
                project != null ? project.getProjectName() : null,
                task.getStatus(),
                task.getEstimatedHours(),
                task.getActualHours(),
                task.getPlannedStartDate(),
                task.getPlannedEndDate(),
                task.getSortOrder(),
                assigneeResults,
                true
        );
    }
}
