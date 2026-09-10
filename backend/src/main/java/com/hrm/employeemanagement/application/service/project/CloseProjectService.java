package com.hrm.employeemanagement.application.service.project;

import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.CloseProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CloseProjectUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectHasUnfinishedTasksException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class CloseProjectService implements CloseProjectUseCase {

    private final LoadProjectPort loadProjectPort;
    private final SaveProjectPort saveProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public CloseProjectService(
            LoadProjectPort loadProjectPort,
            SaveProjectPort saveProjectPort,
            LoadTaskPort loadTaskPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.saveProjectPort = Objects.requireNonNull(saveProjectPort, "SaveProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public ProjectResult closeProject(CloseProjectCommand command) {
        if (command == null || command.projectId() == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_CLOSE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        // Kiểm tra thẩm quyền:
        // 1. Ban giám đốc (VT-01): Được phép đóng hộ, bắt buộc nhập lý do đóng (tối thiểu 10 ký tự)
        // 2. Quản lý dự án (VT-02): Phải là PM trực tiếp phụ trách dự án
        // 3. Admin (VT-06) hoặc các vai trò khác: Không có quyền đóng dự án
        boolean isExecutive = currentUser.getRole() != null && currentUser.getRole().getCode() == RoleCode.VT_01;
        if (isExecutive) {
            if (command.closureReason() == null || command.closureReason().trim().length() < 10) {
                throw new InvalidProjectDataException("Đóng dự án cấp Ban giám đốc bắt buộc phải nhập lý do chi tiết (tối thiểu 10 ký tự)");
            }
        } else {
            Long currentEmployeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                    .map(Employee::getIdValue)
                    .orElse(null);

            if (currentEmployeeId == null || !project.isManagedBy(new EmployeeId(currentEmployeeId))) {
                saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_CLOSE");
                throw new PermissionDeniedException(PermissionCode.PROJECT_CLOSE);
            }
        }

        // Kiểm tra công việc WBS: Mọi task thực thi (TASK) phải ở trạng thái DONE hoặc CANCELLED
        List<Task> allTasks = loadTaskPort.findAllByProjectId(project.getId());
        List<String> unfinishedTaskCodes = allTasks.stream()
                .filter(task -> task.getTaskType() == TaskType.TASK)
                .filter(task -> task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.CANCELLED)
                .map(task -> task.getTaskCode() != null ? task.getTaskCode() : ("ID:" + task.getId().value()))
                .toList();

        if (!unfinishedTaskCodes.isEmpty()) {
            throw new ProjectHasUnfinishedTasksException(
                    "Không thể đóng dự án vì còn công việc chưa hoàn thành: " + String.join(", ", unfinishedTaskCodes),
                    unfinishedTaskCodes);
        }

        // TODO (NCL-04/NCL-05): Tích hợp kiểm tra Bảng chấm công (Timesheet) và Chi phí phát sinh (Expense)
        // khi 2 module này được merge vào hệ thống chính thức.

        // Thực hiện đóng dự án
        project.close(new UserId(currentUserId), command.closureReason());
        Project savedProject = saveProjectPort.save(project);

        // Ghi nhật ký kiểm toán
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "CLOSE_PROJECT",
                "projects",
                savedProject.getIdValue(),
                null,
                "status=CLOSED;reason=" + (command.closureReason() != null ? command.closureReason().trim() : "N/A")));

        return mapToProjectResult(savedProject);
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ACCESS_DENIED",
                "projects",
                projectId,
                null,
                "permission=PROJECT_CLOSE;role=" + (currentUser.getRole() != null ? currentUser.getRole().getCode() : "null")
                        + ";dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }

    private ProjectResult mapToProjectResult(Project project) {
        return new ProjectResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getOrgUnitId(),
                project.getManagerIdValue(),
                project.getStartDate(),
                project.getEndDate(),
                project.getEstimatedHours(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedByValue(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getClosureReason(),
                project.getClosedAt(),
                project.getClosedByValue(),
                project.getReopenReason(),
                project.getReopenedAt(),
                project.getReopenedByValue());
    }
}
