package com.hrm.employeemanagement.application.service.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.task.TaskBoardAssigneeResult;
import com.hrm.employeemanagement.application.dto.task.TaskBoardCardResult;
import com.hrm.employeemanagement.application.dto.task.TaskBoardQuery;
import com.hrm.employeemanagement.application.dto.task.TaskBoardResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskBoardUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;

public class GetTaskBoardService implements GetTaskBoardUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    public GetTaskBoardService(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort,
            LoadOrgUnitPort loadOrgUnitPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "GetAuthenticatedUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.saveDeniedAuditLogPort = saveDeniedAuditLogPort;
    }

    public GetTaskBoardService(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort) {
        this(authenticatedUserPort, loadEmployeePort, loadTaskAssignmentPort, loadTaskPort, loadProjectPort, null, null);
    }

    @Override
    public TaskBoardResult getTaskBoard(TaskBoardQuery query) {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Employee currentEmployee = loadEmployeePort.findByUserId(currentUser.getId()).orElse(null);

        List<Task> rawTasks = fetchRawTasks(query, currentUser, currentEmployee);
        if (rawTasks.isEmpty()) {
            return new TaskBoardResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    query != null ? query.projectId() : null,
                    query != null ? query.employeeId() : null
            );
        }

        // Lọc bỏ CATEGORY - chỉ giữ lại các task cụ thể (TaskType.TASK)
        List<Task> tasks = rawTasks.stream()
                .filter(t -> t.getTaskType() == TaskType.TASK)
                .toList();

        List<TaskId> taskIds = tasks.stream()
                .map(Task::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 1 Batch query tải toàn bộ TaskAssignment (tránh N+1)
        List<TaskAssignment> allAssignments = loadTaskAssignmentPort.findByTaskIdIn(taskIds);
        Map<TaskId, List<TaskAssignment>> assignmentMap = allAssignments.stream()
                .collect(Collectors.groupingBy(TaskAssignment::getTaskId));

        // 1 Batch query tải toàn bộ thông tin nhân sự được giao việc (tránh N+1)
        List<EmployeeId> employeeIds = allAssignments.stream()
                .map(TaskAssignment::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<EmployeeId, Employee> employeeMap = loadEmployeePort.findAllByIdIn(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

        // 1 Batch query tải toàn bộ thông tin dự án liên quan (tránh N+1)
        List<ProjectId> projectIds = tasks.stream()
                .map(Task::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<ProjectId, Project> projectMap = loadProjectPort.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));

        List<TaskBoardCardResult> todoList = new ArrayList<>();
        List<TaskBoardCardResult> inProgressList = new ArrayList<>();
        List<TaskBoardCardResult> inReviewList = new ArrayList<>();
        List<TaskBoardCardResult> doneList = new ArrayList<>();
        List<TaskBoardCardResult> cancelledList = new ArrayList<>();

        for (Task task : tasks) {
            Project project = projectMap.get(task.getProjectId());
            List<TaskAssignment> taskAssignments = assignmentMap.getOrDefault(task.getId(), Collections.emptyList());

            List<TaskBoardAssigneeResult> assignees = taskAssignments.stream()
                    .map(assignment -> {
                        Employee emp = employeeMap.get(assignment.getEmployeeId());
                        return new TaskBoardAssigneeResult(
                                assignment.getEmployeeId() != null ? assignment.getEmployeeId().value() : null,
                                emp != null ? emp.getEmployeeCode() : null,
                                emp != null ? emp.getFullName() : null,
                                assignment.isPrimary()
                        );
                    })
                    .sorted(Comparator.comparing(TaskBoardAssigneeResult::isPrimary).reversed())
                    .toList();

            boolean isAssignedToCurrentUser = currentEmployee != null && taskAssignments.stream()
                    .anyMatch(a -> Objects.equals(a.getEmployeeId(), currentEmployee.getId()));

            boolean isProjectManager = currentEmployee != null && project != null
                    && project.isManagedBy(currentEmployee.getId());

            boolean canMove = isAssignedToCurrentUser || isProjectManager;

            TaskBoardCardResult card = new TaskBoardCardResult(
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
                    assignees,
                    canMove
            );

            TaskStatus status = task.getStatus() != null ? task.getStatus() : TaskStatus.TODO;
            switch (status) {
                case IN_PROGRESS -> inProgressList.add(card);
                case IN_REVIEW -> inReviewList.add(card);
                case DONE -> doneList.add(card);
                case CANCELLED -> cancelledList.add(card);
                case TODO -> todoList.add(card);
            }
        }

        Comparator<TaskBoardCardResult> cardComparator = Comparator
                .comparing(TaskBoardCardResult::sortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TaskBoardCardResult::taskId, Comparator.nullsLast(Comparator.naturalOrder()));

        todoList.sort(cardComparator);
        inProgressList.sort(cardComparator);
        inReviewList.sort(cardComparator);
        doneList.sort(cardComparator);
        cancelledList.sort(cardComparator);

        int total = tasks.size();
        return new TaskBoardResult(
                todoList,
                inProgressList,
                inReviewList,
                doneList,
                cancelledList,
                total,
                query != null ? query.projectId() : null,
                query != null ? query.employeeId() : null
        );
    }

    private List<Task> fetchRawTasks(TaskBoardQuery query, User currentUser, Employee currentEmployee) {
        if (query != null && query.projectId() != null) {
            Project project = loadProjectPort.findById(new ProjectId(query.projectId()))
                    .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + query.projectId()));

            if (!canAccessProject(currentUser, currentEmployee, project)) {
                saveDeniedAudit(
                        currentUser.getIdValue(),
                        "PROJECT_ACCESS_DENIED",
                        "projects",
                        project.getIdValue(),
                        "permission=PROJECT_READ;dataScope=" + currentUser.getDataScope() + ";reason=OUTSIDE_DATA_SCOPE_TASK_BOARD_READ"
                );
                throw new PermissionDeniedException(PermissionCode.PROJECT_READ);
            }

            List<Task> projectTasks = loadTaskPort.findAllByProjectId(project.getId());
            if (query.employeeId() != null) {
                Employee targetEmployee = loadEmployeePort.findById(new EmployeeId(query.employeeId()))
                        .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + query.employeeId()));

                if (!canAccessEmployee(currentUser, currentEmployee, targetEmployee)) {
                    saveDeniedAudit(
                            currentUser.getIdValue(),
                            "EMPLOYEE_ACCESS_DENIED",
                            "employees",
                            targetEmployee.getIdValue(),
                            "permission=EMPLOYEE_READ;dataScope=" + currentUser.getDataScope() + ";reason=OUTSIDE_DATA_SCOPE_TASK_BOARD_READ"
                    );
                    throw new PermissionDeniedException(PermissionCode.EMPLOYEE_READ);
                }

                List<TaskAssignment> assignments = loadTaskAssignmentPort.findByEmployeeId(targetEmployee.getId());
                Set<TaskId> assignedTaskIds = assignments.stream()
                        .map(TaskAssignment::getTaskId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                return projectTasks.stream()
                        .filter(t -> assignedTaskIds.contains(t.getId()))
                        .toList();
            }
            return projectTasks;
        }

        if (query != null && query.employeeId() != null) {
            Employee targetEmployee = loadEmployeePort.findById(new EmployeeId(query.employeeId()))
                    .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự với ID: " + query.employeeId()));

            if (!canAccessEmployee(currentUser, currentEmployee, targetEmployee)) {
                saveDeniedAudit(
                        currentUser.getIdValue(),
                        "EMPLOYEE_ACCESS_DENIED",
                        "employees",
                        targetEmployee.getIdValue(),
                        "permission=EMPLOYEE_READ;dataScope=" + currentUser.getDataScope() + ";reason=OUTSIDE_DATA_SCOPE_TASK_BOARD_READ"
                );
                throw new PermissionDeniedException(PermissionCode.EMPLOYEE_READ);
            }

            List<TaskAssignment> assignments = loadTaskAssignmentPort.findByEmployeeId(targetEmployee.getId());
            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }
            List<TaskId> taskIds = assignments.stream()
                    .map(TaskAssignment::getTaskId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return loadTaskPort.findAllById(taskIds);
        }

        // Mặc định không truyền projectId và employeeId: lấy công việc của nhân viên đang đăng nhập
        if (currentEmployee != null) {
            List<TaskAssignment> assignments = loadTaskAssignmentPort.findByEmployeeId(currentEmployee.getId());
            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }
            List<TaskId> taskIds = assignments.stream()
                    .map(TaskAssignment::getTaskId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return loadTaskPort.findAllById(taskIds);
        }

        return Collections.emptyList();
    }

    private boolean canAccessProject(User currentUser, Employee currentEmployee, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                currentUser.getScopeOrgUnitId() != null
                        && loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = currentEmployee != null ? currentEmployee.getIdValue() : null;
                yield employeeId != null && (project.isManagedBy(new EmployeeId(employeeId))
                        || loadProjectPort.existsMember(project.getIdValue(), employeeId));
            }
        };
    }

    private boolean canAccessEmployee(User currentUser, Employee currentEmployee, Employee targetEmployee) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                targetEmployee.getOrgUnitId() != null
                        && currentUser.getScopeOrgUnitId() != null
                        && loadOrgUnitPort != null
                        && loadOrgUnitPort.existsInOrgUnitBranch(targetEmployee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
            case SELF ->
                currentEmployee != null && Objects.equals(currentEmployee.getId(), targetEmployee.getId());
        };
    }

    private void saveDeniedAudit(Long userId, String action, String tableName, Long recordId, String detail) {
        if (saveDeniedAuditLogPort != null && userId != null) {
            saveDeniedAuditLogPort.save(AuditLog.createChange(
                    userId,
                    action,
                    tableName,
                    recordId,
                    null,
                    detail
            ));
        }
    }
}
