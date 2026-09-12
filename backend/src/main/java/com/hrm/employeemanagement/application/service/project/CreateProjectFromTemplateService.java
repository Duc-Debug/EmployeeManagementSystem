package com.hrm.employeemanagement.application.service.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.projecttemplate.CreateProjectFromTemplateCommand;
import com.hrm.employeemanagement.application.port.inbound.projecttemplate.CreateProjectFromTemplateUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.projecttemplate.LoadProjectTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.projecttemplate.ProjectTemplateNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectCodeGenerator;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplate;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateId;
import com.hrm.employeemanagement.domain.projecttemplate.ProjectTemplateTask;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class CreateProjectFromTemplateService implements CreateProjectFromTemplateUseCase {
    private final LoadProjectTemplatePort loadProjectTemplatePort;
    private final SaveProjectPort saveProjectPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public CreateProjectFromTemplateService(
            LoadProjectTemplatePort loadProjectTemplatePort,
            SaveProjectPort saveProjectPort,
            SaveTaskPort saveTaskPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectTemplatePort = Objects.requireNonNull(loadProjectTemplatePort,
                "LoadProjectTemplatePort không được để trống");
        this.saveProjectPort = Objects.requireNonNull(saveProjectPort, "SaveProjectPort không được để trống");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort không được để trống");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort không được để trống");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort không được để trống");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort không được để trống");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort không được để trống");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort,
                "SaveAuditLogInNewTransactionPort không được để trống");
        this.authorizationService = Objects.requireNonNull(authorizationService,
                "AuthorizationService không được để trống");
    }

    @Override
    public ProjectResult createProjectFromTemplate(CreateProjectFromTemplateCommand command) {
        // 1. Phân quyền & Data Scope (TC-03)
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_CREATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);
        requireOrgUnitInDataScope(currentUser, command.orgUnitId(), PermissionCode.PROJECT_CREATE);

        // 2. Kiểm tra Phòng ban & PM
        OrgUnit orgUnit = loadActiveOrgUnitOrThrow(command.orgUnitId());
        Long resolvedManagerId = resolveManagerId(command.managerId(), currentUser);
        validateManager(resolvedManagerId, command.orgUnitId());

        // 3. Tải Mẫu dự án & Cây công việc mẫu
        ProjectTemplateId templateId = new ProjectTemplateId(command.templateId());
        ProjectTemplate template = loadProjectTemplatePort.findById(templateId)
                .orElseThrow(() -> new ProjectTemplateNotFoundException(command.templateId()));

        if (!template.isActive()) {
            throw new ProjectTemplateNotFoundException("Mẫu dự án đã bị vô hiệu hóa");
        }

        List<ProjectTemplateTask> templateTasks = loadProjectTemplatePort.findTasksByTemplateId(templateId);
        validateTemplateTasksHierarchy(templateTasks);

        // 4. Tự động tính tổng ngân sách giờ từ các task mẫu (taskType == TASK)
        BigDecimal totalEstimatedHours = templateTasks.stream()
                .filter(t -> t.getTaskType() == TaskType.TASK)
                .map(ProjectTemplateTask::getEstimatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Tạo và lưu Dự án mới với mã sinh từ ProjectCodeGenerator
        String generatedCode = ProjectCodeGenerator.generate(orgUnit);
        Project project = Project.createNew(
                generatedCode,
                command.projectName(),
                command.orgUnitId(),
                resolvedManagerId != null ? new EmployeeId(resolvedManagerId) : null,
                command.startDate(),
                command.endDate(),
                totalEstimatedHours,
                command.description(),
                new UserId(currentUserId));

        Project savedProject = saveProjectPort.save(project);

        int clonedCount = cloneTemplateTasksToProject(templateTasks, savedProject, currentUserId);

        String details = String.format("templateId=%d;templateCode=%s;clonedTasksCount=%d;totalEstimatedHours=%s",
                template.getIdValue(), template.getTemplateCode(), clonedCount, totalEstimatedHours);
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "CREATE_PROJECT_FROM_TEMPLATE",
                "projects",
                savedProject.getIdValue(),
                null,
                details));
        return mapToProjectResult(savedProject);
    }

    private void validateTemplateTasksHierarchy(List<ProjectTemplateTask> templateTasks) {
        if (templateTasks == null || templateTasks.isEmpty()) {
            return;
        }
        Set<Long> taskIdsInTemplate = new HashSet<>();
        for (ProjectTemplateTask t : templateTasks) {
            taskIdsInTemplate.add(t.getIdValue());
        }

        for (ProjectTemplateTask t : templateTasks) {
            if (t.getParentId() != null && !taskIdsInTemplate.contains(t.getParentId().value())) {
                throw new InvalidProjectDataException(
                        "Cây công việc của mẫu dự án không hợp lệ: công việc '" + t.getName()
                                + "' có cha không thuộc cùng mẫu dự án");
            }
        }
    }

    private int cloneTemplateTasksToProject(List<ProjectTemplateTask> templateTasks,
            Project project,
            Long currentUserId) {
        if (templateTasks == null || templateTasks.isEmpty()) {
            return 0;
        }
        Map<Long, TaskId> templateTaskIdToNewTaskId = new HashMap<>();
        List<ProjectTemplateTask> remaining = new ArrayList<>(templateTasks);
        // Duyệt theo tầng: node cha (parentId == null hoặc đã được map ID mới) duyệt trước
        while (!remaining.isEmpty()) {
            boolean progress = false;
            Iterator<ProjectTemplateTask> iterator = remaining.iterator();

            while (iterator.hasNext()) {
                ProjectTemplateTask t = iterator.next();
                boolean canProcess = (t.getParentId() == null)
                        || templateTaskIdToNewTaskId.containsKey(t.getParentId().value());

                if (canProcess) {
                    TaskId parentTaskId = t.getParentId() != null
                            ? templateTaskIdToNewTaskId.get(t.getParentId().value())
                            : null;

                    int nextSeq = project.nextTaskSequence();
                    String taskCode = String.format("%s-T%03d", project.getProjectCode(), nextSeq);

                    BigDecimal taskEstimatedHours = (t.getTaskType() == TaskType.CATEGORY)
                            ? BigDecimal.ZERO
                            : (t.getEstimatedHours() != null ? t.getEstimatedHours() : BigDecimal.ZERO);

                    Task newTask = Task.createNew(project.getId(),
                            parentTaskId,
                            taskCode,
                            t.getName(),
                            t.getDescription(),
                            t.getTaskType(),
                            null, // assigneeId luôn là null ban đầu
                            taskEstimatedHours,
                            t.getSortOrder(),
                            new UserId(currentUserId));
                    Task savedTask = saveTaskPort.save(newTask);
                    templateTaskIdToNewTaskId.put(t.getIdValue(), savedTask.getId());

                    iterator.remove();
                    progress = true;
                }
            }
            if (!progress) {
                throw new InvalidProjectDataException(
                        "Cây công việc của mẫu dự án không hợp lệ: tồn tại công việc mồ côi hoặc bị phụ thuộc vòng lặp");
            }
        }

        // Cập nhật lại số đếm taskSeqCounter vào dự án
        saveProjectPort.save(project);
        return templateTaskIdToNewTaskId.size();
    }

    private Long resolveManagerId(Long requestedManagerId, User currentUser) {
        if (requestedManagerId != null) {
            return requestedManagerId;
        }
        if (currentUser.getRole() != null
                && currentUser.getRole().getCode() == com.hrm.employeemanagement.domain.role.RoleCode.VT_02) {
            return loadEmployeePort.findByUserId(currentUser.getId())
                    .map(Employee::getIdValue)
                    .orElse(null);
        }
        return null;
    }

    private void validateManager(Long managerId, Long orgUnitId) {
        if (managerId == null) {
            return;
        }
        Employee manage = loadEmployeePort.findById(new EmployeeId(managerId)).orElseThrow(
                () -> new InvalidProjectDataException("Không tìm thấy nhân viên quản lý dự án với ID: " + managerId));
        if (manage.getStatus() != EmployeeStatus.ACTIVE) {
            throw new InvalidProjectDataException("Nhân viên quản lý dự án không ở trạng thái hoạt động");
        }
        boolean isManagerInOrgUnit = Objects.equals(manage.getOrgUnitId(), orgUnitId) || (manage.getOrgUnitId() != null
                && loadOrgUnitPort.existsInOrgUnitBranch(manage.getOrgUnitId(), orgUnitId));

        if (!isManagerInOrgUnit) {
            throw new InvalidProjectDataException("Người quản lý dự án (PM) phải thuộc đơn vị tổ chức quản lý dự án");
        }
    }

    private void requireOrgUnitInDataScope(User currentUser, Long orgUnitId, PermissionCode permission) {
        if (!isOrgUnitInDataScope(currentUser, orgUnitId)) {
            saveDeniedAudit(currentUser.getIdValue(), currentUser, "OUTSIDE_DATA_SCOPE_ORG_UNIT_" + orgUnitId);
            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        switch (currentUser.getDataScope()) {
            case COMPANY:
                return true;
            case SELF: {
                Long employeeOrgUnitId = loadEmployeePort.findByUserId(currentUser.getId())
                        .map(Employee::getOrgUnitId)
                        .orElse(null);
                return employeeOrgUnitId != null
                        && (loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, employeeOrgUnitId)
                                || loadOrgUnitPort.existsInOrgUnitBranch(employeeOrgUnitId, orgUnitId));
            }
            case ORGANIZATION_BRANCH:
                return loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
            default:
                return false;
        }
    }

    private OrgUnit loadActiveOrgUnitOrThrow(Long orgUnitId) {
        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(orgUnitId)).orElseThrow(
                () -> new InvalidProjectDataException("Không tìm thấy đơn vị tổ chức với ID: " + orgUnitId));
        if (orgUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new InvalidProjectDataException("Đơn vị tổ chức đã bị vô hiệu hóa");
        }
        return orgUnit;
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId)).orElseThrow(
                () -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, String reason) {
        saveDeniedAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ACCESS_DENIED",
                "projects",
                null,
                null,
                "permission=PROJECT_CREATE;dataScope=" + currentUser.getDataScope()
                        + ";scopeOrgUnitId=" + currentUser.getScopeOrgUnitId()
                        + ";reason=" + reason));
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
                project.getUpdatedAt());
    }
}
