package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsCommand;
import com.hrm.employeemanagement.application.dto.task.CloneProjectWbsResult;
import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;
import com.hrm.employeemanagement.application.port.inbound.task.CloneProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.CannotCloneFromSameProjectException;
import com.hrm.employeemanagement.domain.exception.task.EmptySourceWbsException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class CloneProjectWbsService implements CloneProjectWbsUseCase {

    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadProjectPort loadProjectPort;
    private final SaveProjectPort saveProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public CloneProjectWbsService(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadProjectPort loadProjectPort,
            SaveProjectPort saveProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.saveProjectPort = Objects.requireNonNull(saveProjectPort, "SaveProjectPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public CloneProjectWbsResult cloneWbs(CloneProjectWbsCommand command) {
        if (command == null || command.targetProjectId() == null || command.sourceProjectId() == null) {
            throw new InvalidTaskDataException("Mã dự án đích và dự án nguồn không được để trống");
        }

        if (Objects.equals(command.targetProjectId(), command.sourceProjectId())) {
            throw new CannotCloneFromSameProjectException();
        }

        // 1. Kiểm tra quyền hạn PROJECT_WBS_MANAGE
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        // 2. Tải và khóa dự án đích
        Project targetProject = loadProjectPort.findByIdForUpdate(new ProjectId(command.targetProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án đích với ID: " + command.targetProjectId()));

        // Kiểm tra phạm vi dữ liệu (Data Scope)
        if (!canManageWbs(currentUser, currentUserId, targetProject)) {
            saveDeniedAudit(currentUserId, currentUser, targetProject.getIdValue(), "OUTSIDE_DATA_SCOPE_WBS_CLONE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_WBS_MANAGE);
        }

        if (targetProject.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(targetProject.getIdValue());
        }

        // 3. Tải dự án nguồn và kiểm tra quyền hạn / phạm vi dữ liệu (Data Scope)
        Project sourceProject = loadProjectPort.findById(new ProjectId(command.sourceProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án nguồn với ID: " + command.sourceProjectId()));

        if (!canManageWbs(currentUser, currentUserId, sourceProject)) {
            saveDeniedAudit(currentUserId, currentUser, sourceProject.getIdValue(), "OUTSIDE_DATA_SCOPE_WBS_CLONE_SOURCE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_WBS_MANAGE);
        }

        List<Task> sourceTasks = loadTaskPort.findAllByProjectId(sourceProject.getId());
        if (sourceTasks == null || sourceTasks.isEmpty()) {
            throw new EmptySourceWbsException(command.sourceProjectId());
        }

        // 4. Tiến hành sao chép theo cấu trúc cây (Recursive DFS - N tầng)
        Map<Long, List<Task>> childrenByParentId = new HashMap<>();
        List<Task> rootTasks = new ArrayList<>();

        for (Task task : sourceTasks) {
            if (task.getParentIdValue() == null) {
                rootTasks.add(task);
            } else {
                childrenByParentId
                        .computeIfAbsent(task.getParentIdValue(), k -> new ArrayList<>())
                        .add(task);
            }
        }

        Comparator<Task> siblingComparator = Comparator
                .comparingInt(Task::getSortOrder)
                .thenComparing(t -> t.getIdValue() != null ? t.getIdValue() : 0L);

        rootTasks.sort(siblingComparator);
        childrenByParentId.values().forEach(list -> list.sort(siblingComparator));

        Map<Long, TaskId> oldToNewIdMap = new HashMap<>();
        List<Task> clonedTasks = new ArrayList<>();
        int[] categoryCounter = new int[1];

        for (Task rootTask : rootTasks) {
            cloneNodeAndDescendants(
                    rootTask,
                    targetProject,
                    null,
                    currentUserId,
                    childrenByParentId,
                    oldToNewIdMap,
                    clonedTasks,
                    categoryCounter
            );
        }

        // Phòng hộ: Nếu còn task con mồ côi (parentId trỏ ngoài danh sách root)
        if (clonedTasks.size() < sourceTasks.size()) {
            for (Task task : sourceTasks) {
                if (!oldToNewIdMap.containsKey(task.getIdValue())) {
                    TaskId parentId = oldToNewIdMap.get(task.getParentIdValue());
                    cloneNodeAndDescendants(
                            task,
                            targetProject,
                            parentId,
                            currentUserId,
                            childrenByParentId,
                            oldToNewIdMap,
                            clonedTasks,
                            categoryCounter
                    );
                }
            }
        }

        // Cập nhật số đếm task sequence của Target Project
        saveProjectPort.save(targetProject);

        // 5. Ghi nhật ký kiểm toán (NCL-03-CN-008-TC-04)
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "CLONE_PROJECT_WBS",
                "projects",
                targetProject.getIdValue(),
                "sourceProjectId=" + command.sourceProjectId(),
                "clonedTasksCount=" + clonedTasks.size() + ";categories=" + categoryCounter[0]
        ));

        // 6. Trả về cấu trúc cây WBS mới
        List<TaskNodeResult> treeResult = buildTree(clonedTasks);
        return new CloneProjectWbsResult(
                targetProject.getIdValue(),
                command.sourceProjectId(),
                clonedTasks.size(),
                categoryCounter[0],
                treeResult
        );
    }

    private void cloneNodeAndDescendants(
            Task sourceNode,
            Project targetProject,
            TaskId newParentId,
            Long currentUserId,
            Map<Long, List<Task>> childrenByParentId,
            Map<Long, TaskId> oldToNewIdMap,
            List<Task> clonedTasks,
            int[] categoryCounter
    ) {
        Task newTask = cloneSingleTask(sourceNode, targetProject, newParentId, currentUserId);
        Task savedTask = saveTaskPort.save(newTask);
        oldToNewIdMap.put(sourceNode.getIdValue(), savedTask.getId());
        clonedTasks.add(savedTask);
        if (savedTask.isCategory()) {
            categoryCounter[0]++;
        }

        List<Task> children = childrenByParentId.get(sourceNode.getIdValue());
        if (children != null) {
            for (Task child : children) {
                cloneNodeAndDescendants(
                        child,
                        targetProject,
                        savedTask.getId(),
                        currentUserId,
                        childrenByParentId,
                        oldToNewIdMap,
                        clonedTasks,
                        categoryCounter
                );
            }
        }
    }

    private Task cloneSingleTask(Task source, Project targetProject, TaskId newParentId, Long currentUserId) {
        int nextSeq = targetProject.nextTaskSequence();
        String prefix = targetProject.getProjectCode() != null ? targetProject.getProjectCode() : "PRJ";
        String taskCode = String.format("%s-T%03d", prefix, nextSeq);

        // Quy tắc: assigneeId = null, actualHours = 0, status = TODO, budgetHours giữ nguyên
        return new Task(
                null,
                targetProject.getId(),
                newParentId,
                taskCode,
                source.getName(),
                source.getDescription(),
                source.getTaskType(),
                null, // KHÔNG sao chép người phụ trách
                source.getEstimatedHours(),
                BigDecimal.ZERO, // KHÔNG mang theo giờ công thực tế
                source.getBudgetHours(), // Sao chép ngân sách giờ công
                TaskStatus.TODO,
                source.getSortOrder(),
                new UserId(currentUserId),
                java.time.LocalDateTime.now(),
                null,
                null
        );
    }

    private boolean canManageWbs(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY ->
                true;
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
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(
                AuditLog.createChange(
                        currentUserId,
                        "PROJECT_ACCESS_DENIED",
                        "projects",
                        projectId,
                        null,
                        "permission=PROJECT_WBS_MANAGE;dataScope=" + currentUser.getDataScope() + ";reason=" + reason
                )
        );
    }

    private List<TaskNodeResult> buildTree(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        List<Task> sortedTasks = tasks.stream()
                .sorted(Comparator.comparingInt(Task::getSortOrder)
                        .thenComparing(t -> t.getIdValue() != null ? t.getIdValue() : 0L))
                .toList();
        Map<Long, TaskNodeResult> nodeMap = new LinkedHashMap<>();
        for (Task task : sortedTasks) {
            TaskNodeResult node = new TaskNodeResult(
                    task.getIdValue(),
                    task.getProjectIdValue(),
                    task.getParentIdValue(),
                    task.getTaskCode(),
                    task.getName(),
                    task.getDescription(),
                    task.getTaskType(),
                    task.getAssigneeIdValue(),
                    task.getAssigneeIdValue() != null ? List.of(task.getAssigneeIdValue()) : List.of(),
                    task.getEstimatedHours(),
                    task.getActualHours(),
                    task.getBudgetHours(),
                    task.calculateBurnedPercentage(),
                    task.getBudgetBurnStatus(), // Dùng trực tiếp Enum, không gọi .name()
                    task.isOverBudget(),
                    task.getStatus(),
                    task.getSortOrder(),
                    task.getPlannedStartDate(),
                    task.getPlannedEndDate(),
                    task.getStartDate(),
                    task.getDueDate(),
                    task.getActualEndDate(),
                    task.getSlackDays() != null ? task.getSlackDays() : 0,
                    task.getCreatedByValue(),
                    task.getCreatedAt(),
                    task.getUpdatedAt(),
                    task.getVersion(),
                    new ArrayList<>()
            );
            nodeMap.put(task.getIdValue(), node);
        }
        List<TaskNodeResult> rootNodes = new ArrayList<>();
        for (Task task : sortedTasks) {
            TaskNodeResult currentNode = nodeMap.get(task.getIdValue());
            if (task.getParentIdValue() == null) {
                rootNodes.add(currentNode);
            } else {
                TaskNodeResult parentNode = nodeMap.get(task.getParentIdValue());
                if (parentNode != null) {
                    parentNode.children().add(currentNode);
                } else {
                    rootNodes.add(currentNode);
                }
            }
        }
        return rootNodes;
    }
}
