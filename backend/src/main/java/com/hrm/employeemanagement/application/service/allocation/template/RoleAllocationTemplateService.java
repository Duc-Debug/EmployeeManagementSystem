package com.hrm.employeemanagement.application.service.allocation.template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.ApplyRoleAllocationTemplateResult;
import com.hrm.employeemanagement.application.dto.allocation.template.CreateRoleAllocationTemplateCommand;
import com.hrm.employeemanagement.application.dto.allocation.template.PreviewRoleAllocationResult;
import com.hrm.employeemanagement.application.dto.allocation.template.ProjectRoleStructureItem;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateDetailResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateItemResult;
import com.hrm.employeemanagement.application.dto.allocation.template.RoleAllocationTemplateSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.ApplyRoleAllocationTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.CreateRoleAllocationTemplateUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.GetProjectRoleAllocationStructureUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.GetRoleAllocationTemplatesUseCase;
import com.hrm.employeemanagement.application.port.inbound.allocation.template.PreviewRoleAllocationSuggestionUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadProjectRoleAllocationStructurePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.LoadRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.allocation.template.SaveRoleAllocationTemplatePort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplate;
import com.hrm.employeemanagement.domain.allocation.template.ProjectRoleAllocationTemplateItem;
import com.hrm.employeemanagement.domain.allocation.template.RoleAllocationSuggestionItem;
import com.hrm.employeemanagement.domain.allocation.template.RoleAllocationSuggestionPolicy;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.allocation.DuplicateRoleAllocationTemplateCodeException;
import com.hrm.employeemanagement.domain.exception.allocation.RoleAllocationTemplateNotFoundException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.user.UserId;

public class RoleAllocationTemplateService implements
        CreateRoleAllocationTemplateUseCase,
        GetRoleAllocationTemplatesUseCase,
        GetProjectRoleAllocationStructureUseCase,
        PreviewRoleAllocationSuggestionUseCase,
        ApplyRoleAllocationTemplateUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final SaveRoleAllocationTemplatePort saveTemplatePort;
    private final LoadRoleAllocationTemplatePort loadTemplatePort;
    private final LoadProjectRoleAllocationStructurePort loadStructurePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadProjectRolePort loadRolePort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final SaveWeeklyProjectAllocationPort saveAllocationPort;
    private final LoadProjectResourceDemandPort loadDemandPort;
    private final SaveProjectResourceDemandPort saveDemandPort;
    private final SaveAuditLogInNewTransactionPort saveAuditLogPort;

    public RoleAllocationTemplateService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            SaveRoleAllocationTemplatePort saveTemplatePort,
            LoadRoleAllocationTemplatePort loadTemplatePort,
            LoadProjectRoleAllocationStructurePort loadStructurePort,
            LoadProjectPort loadProjectPort,
            LoadProjectRolePort loadRolePort,
            LoadEmployeePort loadEmployeePort,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            SaveWeeklyProjectAllocationPort saveAllocationPort,
            LoadProjectResourceDemandPort loadDemandPort,
            SaveProjectResourceDemandPort saveDemandPort,
            SaveAuditLogInNewTransactionPort saveAuditLogPort
    ) {
        this.authorizationService = authorizationService;
        this.loadUserPort = loadUserPort;
        this.saveTemplatePort = saveTemplatePort;
        this.loadTemplatePort = loadTemplatePort;
        this.loadStructurePort = loadStructurePort;
        this.loadProjectPort = loadProjectPort;
        this.loadRolePort = loadRolePort;
        this.loadEmployeePort = loadEmployeePort;
        this.loadWeeklyAvailabilityPort = loadWeeklyAvailabilityPort;
        this.loadAllocationPort = loadAllocationPort;
        this.saveAllocationPort = saveAllocationPort;
        this.loadDemandPort = loadDemandPort;
        this.saveDemandPort = saveDemandPort;
        this.saveAuditLogPort = saveAuditLogPort;
    }

    private Long requirePermission() {
        try {
            return authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        } catch (PermissionDeniedException ex) {
            // [TC-03] Bị chặn khi không có quyền VT-03 -> Ghi audit log ACCESS_DENIED
            saveAuditLogPort.save(AuditLog.create(
                    null,
                    "ACCESS_DENIED",
                    "ROLE_ALLOCATION_TEMPLATE",
                    null
            ));
            throw ex;
        }
    }

    @Override
    public RoleAllocationTemplateDetailResult createTemplate(CreateRoleAllocationTemplateCommand command) {
        Long currentUserId = requirePermission();

        if (loadTemplatePort.existsByCode(command.templateCode())) {
            throw new DuplicateRoleAllocationTemplateCodeException(command.templateCode());
        }

        List<ProjectRoleAllocationTemplateItem> domainItems = command.items().stream()
                .map(item -> ProjectRoleAllocationTemplateItem.create(item.roleId(), item.hoursPerWeek()))
                .collect(Collectors.toList());

        ProjectRoleAllocationTemplate template = ProjectRoleAllocationTemplate.create(
                command.templateCode(),
                command.name(),
                command.description(),
                command.sourceProjectId(),
                currentUserId,
                domainItems
        );

        ProjectRoleAllocationTemplate saved = saveTemplatePort.save(template);

        // [TC-04] Ghi nhận lịch sử kiểm toán
        saveAuditLogPort.save(AuditLog.create(
                currentUserId,
                "CREATE_ROLE_ALLOCATION_TEMPLATE",
                "ROLE_ALLOCATION_TEMPLATE",
                saved.getId()
        ));

        return toDetailResult(saved);
    }

    @Override
    public List<RoleAllocationTemplateSummaryResult> getAllTemplates() {
        requirePermission();
        return loadTemplatePort.findAll().stream()
                .map(t -> new RoleAllocationTemplateSummaryResult(
                        t.getId(),
                        t.getTemplateCode(),
                        t.getName(),
                        t.getDescription(),
                        t.getSourceProjectId(),
                        t.getItems() != null ? t.getItems().size() : 0,
                        t.getCreatedBy(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public RoleAllocationTemplateDetailResult getTemplateById(Long id) {
        requirePermission();
        ProjectRoleAllocationTemplate template = loadTemplatePort.findById(id)
                .orElseThrow(() -> new RoleAllocationTemplateNotFoundException(id));
        return toDetailResult(template);
    }

    @Override
    public List<ProjectRoleStructureItem> getStructureFromProject(Long projectId) {
        requirePermission();
        loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án: " + projectId));
        return loadStructurePort.extractRoleStructureFromProject(projectId);
    }

    @Override
    public PreviewRoleAllocationResult previewSuggestion(Long templateId, Long targetProjectId) {
        requirePermission();

        ProjectRoleAllocationTemplate template = loadTemplatePort.findById(templateId)
                .orElseThrow(() -> new RoleAllocationTemplateNotFoundException(templateId));

        Project targetProject = loadProjectPort.findById(new ProjectId(targetProjectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án: " + targetProjectId));

        List<YearWeek> targetWeeks = buildTargetWeeks(targetProject);
        List<Employee> activeEmployees = loadEmployeePort.findAllActive();
        List<Long> employeeIds = activeEmployees.stream().map(Employee::getIdValue).collect(Collectors.toList());

        // Batch load availability và allocations trong các tuần dự án
        List<WeeklyAvailability> availabilities = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, BigDecimal> employeeWeekAvailabilityMap = new HashMap<>();
        for (WeeklyAvailability a : availabilities) {
            employeeWeekAvailabilityMap.put(a.getEmployeeId() + "_" + a.getYearWeek().year() + "_" + a.getYearWeek().weekNumber(), a.getNetAvailableHours());
        }

        List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
        Map<String, BigDecimal> employeeWeekAllocatedMap = new HashMap<>();
        for (WeeklyProjectAllocation alloc : allocations) {
            String key = alloc.getEmployeeId() + "_" + alloc.getYear() + "_" + alloc.getWeekNumber();
            employeeWeekAllocatedMap.merge(key, alloc.getAllocatedHours(), BigDecimal::add);
        }

        Map<Long, ProjectRole> roleMap = loadRolePort.findAll().stream()
                .collect(Collectors.toMap(r -> r.getId().value(), r -> r, (r1, r2) -> r1));

        List<PreviewRoleAllocationResult.RoleSuggestionItemResult> suggestionResults = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean hasUnassigned = false;

        for (ProjectRoleAllocationTemplateItem item : template.getItems()) {
            Long roleId = item.getRoleId();
            ProjectRole role = roleMap.get(roleId);
            String roleCode = role != null ? role.getCode() : "ROLE_" + roleId;
            String roleName = role != null ? role.getName() : "Vai trò " + roleId;
            BigDecimal requiredHours = item.getHoursPerWeek();

            // Tìm nhân sự phù hợp theo vai trò (chuyên môn)
            List<Employee> matchingEmployees = activeEmployees.stream()
                    .filter(e -> e.getProfessionalRole() != null &&
                            (e.getProfessionalRole().equalsIgnoreCase(roleCode) ||
                             e.getProfessionalRole().equalsIgnoreCase(roleName)))
                    .collect(Collectors.toList());

            // Đánh giá độ rảnh trong tất cả các tuần của dự án
            List<RoleAllocationSuggestionPolicy.CandidateAvailability> candidateAvailabilities = new ArrayList<>();
            for (Employee candidate : matchingEmployees) {
                BigDecimal minRemaining = null;
                for (YearWeek yw : targetWeeks) {
                    String key = candidate.getIdValue() + "_" + yw.year() + "_" + yw.weekNumber();
                    BigDecimal netAvail = employeeWeekAvailabilityMap.getOrDefault(key,
                            BigDecimal.valueOf(candidate.getStandardHoursPerWeek() != null ? candidate.getStandardHoursPerWeek() : 40));
                    BigDecimal allocated = employeeWeekAllocatedMap.getOrDefault(key, BigDecimal.ZERO);
                    BigDecimal remaining = netAvail.subtract(allocated);

                    if (minRemaining == null || remaining.compareTo(minRemaining) < 0) {
                        minRemaining = remaining;
                    }
                }
                candidateAvailabilities.add(new RoleAllocationSuggestionPolicy.CandidateAvailability(
                        candidate.getIdValue(),
                        candidate.getFullName(),
                        candidate.getEmployeeCode(),
                        minRemaining != null ? minRemaining : BigDecimal.ZERO
                ));
            }

            // Gợi ý theo Policy
            RoleAllocationSuggestionItem suggestion = RoleAllocationSuggestionPolicy.suggestCandidateForRole(
                    roleId,
                    roleCode,
                    roleName,
                    requiredHours,
                    candidateAvailabilities
            );

            if (!suggestion.isAssigned()) {
                hasUnassigned = true;
                warnings.add(suggestion.getWarningMessage());
            }

            suggestionResults.add(new PreviewRoleAllocationResult.RoleSuggestionItemResult(
                    roleId,
                    roleCode,
                    roleName,
                    requiredHours,
                    suggestion.getSuggestedEmployeeId(),
                    suggestion.getSuggestedEmployeeName(),
                    suggestion.getSuggestedEmployeeCode(),
                    suggestion.isAssigned(),
                    suggestion.getWarningMessage()
            ));
        }

        return new PreviewRoleAllocationResult(
                template.getId(),
                template.getTemplateCode(),
                template.getName(),
                targetProject.getId().value(),
                targetProject.getProjectName(),
                targetWeeks.size(),
                suggestionResults,
                hasUnassigned,
                warnings
        );
    }

    @Override
    public ApplyRoleAllocationTemplateResult applyTemplate(ApplyRoleAllocationTemplateCommand command) {
        Long currentUserId = requirePermission();

        ProjectRoleAllocationTemplate template = loadTemplatePort.findById(command.templateId())
                .orElseThrow(() -> new RoleAllocationTemplateNotFoundException(command.templateId()));

        Project targetProject = loadProjectPort.findById(new ProjectId(command.targetProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án: " + command.targetProjectId()));

        List<YearWeek> targetWeeks = buildTargetWeeks(targetProject);
        Map<Long, ProjectRole> roleMap = loadRolePort.findAll().stream()
                .collect(Collectors.toMap(r -> r.getId().value(), r -> r, (r1, r2) -> r1));

        int appliedRolesCount = 0;
        int allocatedEmployeesCount = 0;
        int unassignedRolesCount = 0;
        List<String> warnings = new ArrayList<>();

        if (command.assignments() != null) {
            for (ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand item : command.assignments()) {
                appliedRolesCount++;
                Long roleId = item.roleId();
                BigDecimal hoursPerWeek = item.hoursPerWeek();
                ProjectRole role = roleMap.get(roleId);
                String roleName = role != null ? role.getName() : "Vai trò " + roleId;

                // 1. Tạo/cập nhật nhu cầu nhân sự (ProjectResourceDemand) cho từng tuần của dự án
                for (YearWeek yw : targetWeeks) {
                    ProjectResourceDemand demand = ProjectResourceDemand.createNew(
                            targetProject.getId(),
                            new ProjectRoleId(roleId),
                            yw,
                            hoursPerWeek
                    );
                    saveDemandPort.save(demand);
                }

                // 2. Nếu có gán nhân sự -> Tạo WeeklyProjectAllocation cho từng tuần
                if (item.employeeId() != null) {
                    allocatedEmployeesCount++;
                    for (YearWeek yw : targetWeeks) {
                        Optional<WeeklyProjectAllocation> existingAlloc = loadAllocationPort.loadAllocation(item.employeeId(), targetProject.getId().value(), yw);
                        if (existingAlloc.isPresent()) {
                            WeeklyProjectAllocation alloc = existingAlloc.get();
                            alloc.updateAllocatedHours(alloc.getAllocatedHours().add(hoursPerWeek));
                            saveAllocationPort.save(alloc);
                        } else {
                            WeeklyProjectAllocation newAlloc = WeeklyProjectAllocation.createNew(
                                    item.employeeId(),
                                    targetProject.getId().value(),
                                    yw,
                                    hoursPerWeek
                            );
                            saveAllocationPort.save(newAlloc);
                        }
                    }
                } else {
                    unassignedRolesCount++;
                    warnings.add("Cần bổ sung nhân sự cho vai trò " + roleName);
                }
            }
        }

        // [TC-04] Ghi nhận lịch sử kiểm toán thao tác áp mẫu
        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "APPLY_ROLE_ALLOCATION_TEMPLATE",
                "ROLE_ALLOCATION_TEMPLATE",
                template.getId(),
                null,
                "Applied to project " + targetProject.getId().value() + ": " + appliedRolesCount + " roles"
        ));

        String message = unassignedRolesCount > 0
                ? "Áp mẫu hoàn tất. Có " + unassignedRolesCount + " vai trò chưa được phân bổ nhân sự (cần bổ sung)."
                : "Áp mẫu phân bổ vai trò vào dự án thành công!";

        return new ApplyRoleAllocationTemplateResult(
                template.getId(),
                targetProject.getId().value(),
                appliedRolesCount,
                allocatedEmployeesCount,
                unassignedRolesCount,
                warnings,
                message
        );
    }

    private List<YearWeek> buildTargetWeeks(Project project) {
        List<YearWeek> weeks = new ArrayList<>();
        LocalDate start = project.getStartDate() != null ? project.getStartDate() : LocalDate.now();
        LocalDate end = project.getEndDate() != null ? project.getEndDate() : start.plusWeeks(4);

        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            YearWeek yw = YearWeek.from(curr);
            if (!weeks.contains(yw)) {
                weeks.add(yw);
            }
            curr = curr.plusWeeks(1);
        }
        return weeks;
    }

    private RoleAllocationTemplateDetailResult toDetailResult(ProjectRoleAllocationTemplate template) {
        Map<Long, ProjectRole> roleMap = loadRolePort.findAll().stream()
                .collect(Collectors.toMap(r -> r.getId().value(), r -> r, (r1, r2) -> r1));

        List<RoleAllocationTemplateItemResult> itemResults = template.getItems() != null
                ? template.getItems().stream().map(item -> {
                    ProjectRole role = roleMap.get(item.getRoleId());
                    return new RoleAllocationTemplateItemResult(
                            item.getId(),
                            item.getRoleId(),
                            role != null ? role.getCode() : "ROLE_" + item.getRoleId(),
                            role != null ? role.getName() : "Vai trò " + item.getRoleId(),
                            item.getHoursPerWeek()
                    );
                }).collect(Collectors.toList())
                : List.of();

        return new RoleAllocationTemplateDetailResult(
                template.getId(),
                template.getTemplateCode(),
                template.getName(),
                template.getDescription(),
                template.getSourceProjectId(),
                template.getCreatedBy(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                template.getVersion(),
                itemResults
        );
    }
}
