package com.hrm.employeemanagement.application.service.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.task.TaskBoardAssigneeResult;
import com.hrm.employeemanagement.application.dto.task.tracking.ProjectTaskTrackingResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingItemResult;
import com.hrm.employeemanagement.application.dto.task.tracking.TaskTrackingQuery;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectTaskTrackingUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskOverduePolicy;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Triển khai Use Case Bảng theo dõi công việc của dự án (NCL-04-CN-003).
 * Tuân thủ quy tắc QTN-01 (Phân quyền & Kiểm toán) và QTN-04 (Dự án đang chạy).
 */
public class GetProjectTaskTrackingService implements GetProjectTaskTrackingUseCase {

    private static final String EMPTY_TASK_SUGGESTION =
            "Dự án chưa có công việc nào. Gợi ý: Hãy tạo cây công việc (WBS) để bắt đầu theo dõi tiến độ.";

    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;

    public GetProjectTaskTrackingService(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public ProjectTaskTrackingResult getTaskTracking(TaskTrackingQuery query) {
        if (query == null || query.projectId() == null) {
            throw new InvalidTaskDataException("Mã dự án (projectId) không được để trống");
        }

        // 1. Phân quyền & Kiểm tra tài khoản (TC-04 & QTN-01)
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));

        Project project = loadProjectPort.findById(new ProjectId(query.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + query.projectId()));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, query.projectId(), "OUTSIDE_DATA_SCOPE_TASK_TRACKING_READ");
            throw new PermissionDeniedException(PermissionCode.PROJECT_READ);
        }

        // 2. Tuân thủ quy tắc nghiệp vụ QTN-04: Dự án phải đang chạy, từ chối khi dự án đã đóng
        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        // 3. Tải toàn bộ công việc và hạng mục của dự án
        List<Task> allProjectTasks = loadTaskPort.findAllByProjectId(project.getId());
        if (allProjectTasks == null || allProjectTasks.isEmpty()) {
            return emptyTrackingResult(project);
        }

        // Tách nhóm hạng mục (CATEGORY) để tra cứu tên hạng mục cha
        Map<TaskId, String> categoryNameMap = allProjectTasks.stream()
                .filter(t -> t.getTaskType() == TaskType.CATEGORY && t.getId() != null)
                .collect(Collectors.toMap(Task::getId, Task::getName, (existing, replacement) -> existing));

        // Lọc danh sách công việc cụ thể (TASK)
        List<Task> taskItems = allProjectTasks.stream()
                .filter(t -> t.getTaskType() == TaskType.TASK)
                .toList();

        if (taskItems.isEmpty()) {
            return emptyTrackingResult(project);
        }

        List<TaskId> taskIds = taskItems.stream()
                .map(Task::getId)
                .filter(Objects::nonNull)
                .toList();

        // 4. Batch query tối ưu tải thông tin phân công (TaskAssignment) - Tránh N+1
        List<TaskAssignment> allAssignments = loadTaskAssignmentPort.findByTaskIdIn(taskIds);
        Map<TaskId, List<TaskAssignment>> assignmentMap = allAssignments.stream()
                .collect(Collectors.groupingBy(TaskAssignment::getTaskId));

        // 5. Batch query tối ưu tải thông tin nhân sự (Employee) - Tránh N+1
        List<EmployeeId> employeeIds = allAssignments.stream()
                .map(TaskAssignment::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<EmployeeId, Employee> employeeMap = employeeIds.isEmpty()
                ? Collections.emptyMap()
                : loadEmployeePort.findAllByIdIn(employeeIds).stream()
                        .collect(Collectors.toMap(Employee::getId, e -> e, (existing, replacement) -> existing));

        LocalDate today = LocalDate.now();
        List<TaskTrackingItemResult> items = new ArrayList<>();

        for (Task task : taskItems) {
            List<TaskAssignment> taskAssignments = assignmentMap.getOrDefault(task.getId(), Collections.emptyList());

            List<TaskBoardAssigneeResult> assignees = taskAssignments.stream()
                    .map(a -> {
                        Employee emp = employeeMap.get(a.getEmployeeId());
                        return new TaskBoardAssigneeResult(
                                a.getEmployeeId() != null ? a.getEmployeeId().value() : null,
                                emp != null ? emp.getEmployeeCode() : null,
                                emp != null ? emp.getFullName() : null,
                                a.isPrimary()
                        );
                    })
                    .sorted(Comparator.comparing(TaskBoardAssigneeResult::isPrimary).reversed())
                    .toList();

            // Áp dụng bộ lọc theo nhân sự (filter by employeeId)
            if (query.employeeId() != null) {
                boolean hasEmployee = taskAssignments.stream()
                        .anyMatch(a -> a.getEmployeeId() != null && Objects.equals(a.getEmployeeId().value(), query.employeeId()));
                if (!hasEmployee && (task.getAssigneeIdValue() == null || !Objects.equals(task.getAssigneeIdValue(), query.employeeId()))) {
                    continue;
                }
            }

            // Áp dụng bộ lọc theo trạng thái (filter by status)
            if (query.status() != null && task.getStatus() != query.status()) {
                continue;
            }

            // Tính toán quá hạn theo Domain Policy
            boolean isOverdue = TaskOverduePolicy.isOverdue(task, today);
            long overdueDays = TaskOverduePolicy.calculateOverdueDays(task, today);

            // Áp dụng bộ lọc chỉ lấy quá hạn (filter by overdueOnly)
            if (Boolean.TRUE.equals(query.overdueOnly()) && !isOverdue) {
                continue;
            }

            String categoryName = task.getParentId() != null
                    ? categoryNameMap.getOrDefault(task.getParentId(), "Chưa phân hạng mục")
                    : "Chưa phân hạng mục";

            TaskTrackingItemResult item = new TaskTrackingItemResult(
                    task.getIdValue(),
                    task.getTaskCode(),
                    task.getName(),
                    task.getDescription(),
                    task.getParentIdValue(),
                    categoryName,
                    assignees,
                    task.getStatus() != null ? task.getStatus() : TaskStatus.TODO,
                    task.getPlannedStartDate() != null ? task.getPlannedStartDate() : task.getStartDate(),
                    task.getPlannedEndDate() != null ? task.getPlannedEndDate() : task.getDueDate(),
                    task.getBudgetHours() != null ? task.getBudgetHours() : BigDecimal.ZERO,
                    task.getActualHours() != null ? task.getActualHours() : BigDecimal.ZERO,
                    task.calculateBurnedPercentage(),
                    isOverdue,
                    overdueDays,
                    task.getSortOrder() != null ? task.getSortOrder() : 0
            );

            items.add(item);
        }

        // 6. TC-02: Sắp xếp các công việc quá hạn lên ĐẦU DANH SÁCH theo số ngày quá hạn giảm dần
        Comparator<TaskTrackingItemResult> overdueFirstComparator = Comparator
                .comparing(TaskTrackingItemResult::isOverdue).reversed()
                .thenComparing(TaskTrackingItemResult::overdueDays, Comparator.reverseOrder())
                .thenComparing(TaskTrackingItemResult::sortOrder)
                .thenComparing(TaskTrackingItemResult::taskId);

        items.sort(overdueFirstComparator);

        // 7. Thống kê tổng hợp số liệu
        int totalTasks = items.size();
        int overdueTasks = (int) items.stream().filter(TaskTrackingItemResult::isOverdue).count();
        int completedTasks = (int) items.stream().filter(i -> i.status() == TaskStatus.DONE).count();
        int inProgressTasks = (int) items.stream().filter(i -> i.status() == TaskStatus.IN_PROGRESS).count();

        BigDecimal totalBudgetHours = items.stream()
                .map(TaskTrackingItemResult::budgetHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalActualHours = items.stream()
                .map(TaskTrackingItemResult::actualHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String suggestionMessage = totalTasks == 0 ? EMPTY_TASK_SUGGESTION : null;

        return new ProjectTaskTrackingResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getStatus(),
                totalTasks,
                overdueTasks,
                completedTasks,
                inProgressTasks,
                totalBudgetHours,
                totalActualHours,
                suggestionMessage,
                items
        );
    }

    private ProjectTaskTrackingResult emptyTrackingResult(Project project) {
        return new ProjectTaskTrackingResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getStatus(),
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EMPTY_TASK_SUGGESTION,
                Collections.emptyList()
        );
    }

    private boolean canAccessProject(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                currentUser.getScopeOrgUnitId() != null
                        && loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                        .map(Employee::getIdValue)
                        .orElse(null);
                yield employeeId != null && (project.isManagedBy(new EmployeeId(employeeId))
                        || loadProjectPort.existsMember(project.getIdValue(), employeeId));
            }
        };
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(
                AuditLog.createChange(
                        currentUserId,
                        "PROJECT_ACCESS_DENIED",
                        "projects",
                        projectId,
                        null,
                        "permission=PROJECT_READ;dataScope=" + currentUser.getDataScope() + ";reason=" + reason
                )
        );
    }
}
