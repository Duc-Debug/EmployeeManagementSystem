package com.hrm.employeemanagement.application.service.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.task.AssignTaskCommand;
import com.hrm.employeemanagement.application.dto.task.TaskAssignmentResult;
import com.hrm.employeemanagement.application.port.inbound.task.AssignTaskUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.AssigneeInactiveException;
import com.hrm.employeemanagement.domain.exception.task.AssigneeNotInProjectException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class AssignTaskService implements AssignTaskUseCase {

    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final SaveTaskAssignmentPort saveTaskAssignmentPort;
    private final LoadProjectPort loadProjectPort;
    private final SaveProjectMemberPort saveProjectMemberPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public AssignTaskService(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            SaveTaskAssignmentPort saveTaskAssignmentPort,
            LoadProjectPort loadProjectPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.saveTaskAssignmentPort = Objects.requireNonNull(saveTaskAssignmentPort, "SaveTaskAssignmentPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.saveProjectMemberPort = saveProjectMemberPort;
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = saveAuditLogPort;
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    public AssignTaskService(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            SaveTaskAssignmentPort saveTaskAssignmentPort,
            LoadProjectPort loadProjectPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this(loadTaskPort, saveTaskPort, loadTaskAssignmentPort, saveTaskAssignmentPort, loadProjectPort, saveProjectMemberPort,
                loadEmployeePort, null, loadUserPort, saveAuditLogPort, saveDeniedAuditLogPort, authorizationService);
    }

    @Override
    public TaskAssignmentResult assignTask(AssignTaskCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        var project = loadProjectPort.findByIdForUpdate(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (!canManageWbs(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_WBS_MANAGE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_WBS_MANAGE);
        }

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException("Không thể phân công công việc trong dự án đã đóng.");
        }

        Task task = loadTaskPort.findById(new TaskId(command.taskId()))
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        if (!Objects.equals(task.getProjectIdValue(), command.projectId())) {
            throw new TaskNotFoundException(command.taskId());
        }

        if (task.getTaskType() == TaskType.CATEGORY) {
            throw new InvalidTaskDataException("Không thể phân công người thực hiện cho hạng mục (CATEGORY).");
        }

        List<Long> employeeIds = command.employeeIds() != null
                ? new ArrayList<>(new java.util.LinkedHashSet<>(command.employeeIds().stream().filter(Objects::nonNull).toList()))
                : List.of();
        List<EmployeeId> validEmployeeIds = new ArrayList<>();

        for (Long empId : employeeIds) {
            Employee employee = loadEmployeePort.findById(new EmployeeId(empId))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy nhân sự với ID: " + empId));

            if (employee.getStatus() != null && employee.getStatus() != EmployeeStatus.ACTIVE) {
                throw new AssigneeInactiveException("Nhân sự [" + employee.getFullName() + "] không ở trạng thái hoạt động (ACTIVE).");
            }

            // Enforce data scope on employee
            if (currentUser.getDataScope() == DataScope.ORGANIZATION_BRANCH && loadOrgUnitPort != null) {
                if (employee.getOrgUnitId() == null || !loadOrgUnitPort.existsInOrgUnitBranch(employee.getOrgUnitId(), currentUser.getScopeOrgUnitId())) {
                    saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "ASSIGNEE_OUTSIDE_DATA_SCOPE");
                    throw new PermissionDeniedException(PermissionCode.PROJECT_WBS_MANAGE);
                }
            }

            java.time.LocalDate plannedStart = command.plannedStartDate() != null ? command.plannedStartDate() : task.getPlannedStartDate();
            if (plannedStart != null && employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(plannedStart)) {
                throw new AssigneeInactiveException("Nhân sự [" + employee.getFullName() + "] đã kết thúc hợp đồng lao động (" + employee.getContractEndDate() + ") trước ngày bắt đầu công việc (" + plannedStart + ").");
            } else if (plannedStart == null && employee.getContractEndDate() != null && employee.getContractEndDate().isBefore(java.time.LocalDate.now())) {
                throw new AssigneeInactiveException("Nhân sự [" + employee.getFullName() + "] đã hết hạn hợp đồng lao động (" + employee.getContractEndDate() + ").");
            }

            boolean isMember = (project.getManagerId() != null && Objects.equals(project.getManagerId().value(), empId))
                    || loadProjectPort.existsMember(command.projectId(), empId);

            if (!isMember) {
                if (saveProjectMemberPort != null) {
                    saveProjectMemberPort.addMember(command.projectId(), empId);
                    if (saveAuditLogPort != null) {
                        saveAuditLogPort.save(AuditLog.create(currentUserId, "ADD_PROJECT_MEMBER", "project_members", command.projectId()));
                    }
                } else {
                    throw new AssigneeNotInProjectException(empId, command.projectId());
                }
            }

            validEmployeeIds.add(new EmployeeId(empId));
        }

        // Cập nhật ngày mong muốn và người phụ trách chính (primary) trên Task
        task.updatePlannedDates(command.plannedStartDate(), command.plannedEndDate());
        EmployeeId primaryAssignee = validEmployeeIds.isEmpty() ? null : validEmployeeIds.get(0);
        task.assignTo(primaryAssignee);
        saveTaskPort.save(task);

        // Xóa những người không còn được phân công
        if (validEmployeeIds.isEmpty()) {
            saveTaskAssignmentPort.deleteByTaskId(task.getId());
        } else {
            saveTaskAssignmentPort.deleteByTaskIdAndEmployeeIdNotIn(task.getId(), validEmployeeIds);
        }

        // Lưu danh sách phân công mới
        for (int i = 0; i < validEmployeeIds.size(); i++) {
            EmployeeId empId = validEmployeeIds.get(i);
            boolean isPrimary = (i == 0);
            var existingAssignment = loadTaskAssignmentPort.findByTaskIdAndEmployeeId(task.getId(), empId);

            if (existingAssignment.isPresent()) {
                TaskAssignment assignment = existingAssignment.get();
                assignment.setPrimary(isPrimary);
                saveTaskAssignmentPort.save(assignment);
            } else {
                TaskAssignment newAssignment = TaskAssignment.create(
                        task.getId(),
                        empId,
                        currentUserId != null ? new UserId(currentUserId) : null,
                        isPrimary
                );
                saveTaskAssignmentPort.save(newAssignment);
            }
        }

        return new TaskAssignmentResult(
                task.getIdValue(),
                task.getTaskCode(),
                task.getName(),
                employeeIds,
                task.getPlannedStartDate(),
                task.getPlannedEndDate()
        );
    }

    private boolean canManageWbs(User currentUser, Long currentUserId, com.hrm.employeemanagement.domain.project.Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                        .map(Employee::getIdValue)
                        .orElse(null);
                yield employeeId != null && project.isManagedBy(new EmployeeId(employeeId));
            }
        };
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId)).orElseThrow(
                () -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(
                AuditLog.createChange(currentUserId,
                        "PROJECT_ACCESS_DENIED",
                        "projects",
                        projectId,
                        null,
                        "permission=PROJECT_WBS_MANAGE;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }
}
