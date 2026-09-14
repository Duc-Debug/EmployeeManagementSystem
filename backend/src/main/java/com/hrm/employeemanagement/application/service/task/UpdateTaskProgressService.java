package com.hrm.employeemanagement.application.service.task;

import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.task.TaskProgressResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskProgressCommand;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskProgressUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
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
import com.hrm.employeemanagement.domain.task.TaskProgressPolicy;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.user.User;

public class UpdateTaskProgressService implements UpdateTaskProgressUseCase {

    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    public UpdateTaskProgressService(
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
    public TaskProgressResult updateProgress(UpdateTaskProgressCommand command) {
        if (command == null || command.taskId() == null || command.status() == null) {
            throw new InvalidTaskDataException("Mã công việc (taskId) và trạng thái mới (status) không được để trống");
        }

        // 1. Quy tắc nghiệp vụ miền: Xác thực trạng thái hợp lệ cho nhân viên chuyên môn (Whitelist)
        TaskProgressPolicy.validateProgressStatus(command.status());

        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Task task = loadTaskPort.findById(new TaskId(command.taskId()))
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        // 2. Fail-Fast Authorization (TC-02): Kiểm tra phân công trước để tránh truy vấn DB bảng projects khi không có quyền.
        // TaskAssignment là Single Source of Truth (SSOT). Chỉ fallback về task.assigneeId khi task chưa có dữ liệu trong task_assignments (dữ liệu cũ/legacy).
        Employee currentEmployee = loadEmployeePort.findByUserId(currentUser.getId()).orElse(null);
        List<TaskAssignment> assignments = loadTaskAssignmentPort.findByTaskId(task.getId());

        boolean isAssigned;
        if (!assignments.isEmpty()) {
            isAssigned = currentEmployee != null && assignments.stream()
                    .anyMatch(a -> Objects.equals(a.getEmployeeId(), currentEmployee.getId()));
        } else {
            isAssigned = currentEmployee != null && task.getAssigneeId() != null
                    && Objects.equals(task.getAssigneeId(), currentEmployee.getId());
        }

        if (!isAssigned) {
            saveDeniedAuditLogPort.save(AuditLog.createChange(
                    currentUser.getIdValue(),
                    "TASK_ACCESS_DENIED",
                    "tasks",
                    task.getIdValue(),
                    task.getStatus() != null ? task.getStatus().name() : null,
                    "reason=NOT_ASSIGNED_TO_USER;targetStatus=" + command.status().name()
            ));
            throw new TaskNotAssignedToUserException(task.getIdValue(), currentEmployee != null ? currentEmployee.getIdValue() : null);
        }

        // 3. Tải dự án và kiểm tra quy tắc QTN-08
        Project project = loadProjectPort.findById(task.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + task.getProjectIdValue()));

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        TaskStatus oldStatus = task.getStatus();

        // 4. Cập nhật trạng thái trùng nhau -> No-op (idempotent, không ghi DB, không ghi audit log thừa)
        if (oldStatus == command.status()) {
            return new TaskProgressResult(
                    task.getIdValue(),
                    task.getProjectIdValue(),
                    project.getProjectCode(),
                    project.getProjectName(),
                    task.getTaskCode(),
                    task.getName(),
                    oldStatus,
                    task.getStatus(),
                    task.getUpdatedAt()
            );
        }

        // 5. NCL-04-CN-002-TC-01: Cập nhật trạng thái công việc
        task.updateStatus(command.status());
        Task savedTask = saveTaskPort.save(task);

        // 6. NCL-04-CN-002-TC-03: Ghi nhận nhật ký kiểm toán thay đổi tiến độ công việc
        saveAuditLogPort.save(AuditLog.createChange(
                currentUser.getIdValue(),
                "UPDATE_TASK_PROGRESS",
                "tasks",
                savedTask.getIdValue(),
                oldStatus != null ? oldStatus.name() : null,
                command.status().name()
        ));

        return new TaskProgressResult(
                savedTask.getIdValue(),
                savedTask.getProjectIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                savedTask.getTaskCode(),
                savedTask.getName(),
                oldStatus,
                savedTask.getStatus(),
                savedTask.getUpdatedAt()
        );
    }
}
