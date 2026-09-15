package com.hrm.employeemanagement.application.service.allocation.template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
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
import com.hrm.employeemanagement.domain.exception.allocation.InvalidRoleAllocationTemplateException;
import com.hrm.employeemanagement.domain.exception.allocation.ProjectInactiveException;
import com.hrm.employeemanagement.domain.exception.allocation.RoleAllocationTemplateNotFoundException;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.User;

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
    private final LoadOrgUnitPort loadOrgUnitPort;
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
            LoadOrgUnitPort loadOrgUnitPort,
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
        this.loadOrgUnitPort = loadOrgUnitPort;
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
        User currentUser = loadCurrentUser(currentUserId);

        if (command.sourceProjectId() != null) {
            Project sourceProject = loadProjectPort.findById(new ProjectId(command.sourceProjectId()))
                    .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án nguồn: " + command.sourceProjectId()));
            requireProjectInDataScope(currentUser, sourceProject);
        }

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
        Long currentUserId = requirePermission();
        User currentUser = loadCurrentUser(currentUserId);
        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án: " + projectId));
        requireProjectInDataScope(currentUser, project);
        return loadStructurePort.extractRoleStructureFromProject(projectId);
    }

    @Override
    public PreviewRoleAllocationResult previewSuggestion(Long templateId, Long targetProjectId) {
        Long currentUserId = requirePermission();
        User currentUser = loadCurrentUser(currentUserId);

        ProjectRoleAllocationTemplate template = loadTemplatePort.findById(templateId)
                .orElseThrow(() -> new RoleAllocationTemplateNotFoundException(templateId));

        Project targetProject = loadProjectPort.findById(new ProjectId(targetProjectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án: " + targetProjectId));
        requireProjectInDataScope(currentUser, targetProject);
        requireActiveProject(targetProject);

        List<YearWeek> targetWeeks = buildTargetWeeks(targetProject);
        List<Employee> activeEmployees = loadEmployeePort.findAllActive().stream()
                .filter(employee -> isOrgUnitInDataScope(currentUser, employee.getOrgUnitId()))
                .collect(Collectors.toList());
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

        // Track suggestions made during this preview so later roles see the
        // employee's reduced capacity instead of independently reusing the
        // original capacity for every role.
        Map<String, BigDecimal> employeeWeekSuggestedMap = new HashMap<>();

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
                    BigDecimal netAvail = employeeWeekAvailabilityMap.getOrDefault(key, resolveAvailableHours(candidate, yw));
                    BigDecimal allocated = employeeWeekAllocatedMap.getOrDefault(key, BigDecimal.ZERO);
                    BigDecimal suggested = employeeWeekSuggestedMap.getOrDefault(key, BigDecimal.ZERO);
                    BigDecimal remaining = netAvail.subtract(allocated).subtract(suggested);

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

            if (suggestion.isAssigned()) {
                for (YearWeek yw : targetWeeks) {
                    String key = suggestion.getSuggestedEmployeeId() + "_" + yw.year() + "_" + yw.weekNumber();
                    employeeWeekSuggestedMap.merge(key, requiredHours, BigDecimal::add);
                }
            }

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
        User currentUser = loadCurrentUser(currentUserId);

        ProjectRoleAllocationTemplate template = loadTemplatePort.findById(command.templateId())
                .orElseThrow(() -> new RoleAllocationTemplateNotFoundException(command.templateId()));

        Project targetProject = loadProjectPort.findById(new ProjectId(command.targetProjectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án: " + command.targetProjectId()));
        requireProjectInDataScope(currentUser, targetProject);
        requireActiveProject(targetProject);

        List<YearWeek> targetWeeks = buildTargetWeeks(targetProject);
        Map<Long, ProjectRole> roleMap = loadRolePort.findAll().stream()
                .collect(Collectors.toMap(r -> r.getId().value(), r -> r, (r1, r2) -> r1));

        List<ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand> assignments =
                command.assignments() != null ? command.assignments() : List.of();
        Map<Long, ProjectRoleAllocationTemplateItem> templateItems = template.getItems().stream()
                .collect(Collectors.toMap(ProjectRoleAllocationTemplateItem::getRoleId, item -> item));
        validateAssignments(assignments, templateItems, roleMap);

        Map<Long, Employee> employees = loadEmployeePort.findAllActive().stream()
                .collect(Collectors.toMap(Employee::getIdValue, employee -> employee, (a, b) -> a));
        Map<Long, BigDecimal> desiredHoursByEmployee = new HashMap<>();
        for (ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand item : assignments) {
            if (item.employeeId() != null) {
                Employee employee = employees.get(item.employeeId());
                ProjectRole role = roleMap.get(item.roleId());
                validateEmployeeAssignment(currentUser, employee, role, item.employeeId());
                desiredHoursByEmployee.merge(item.employeeId(), item.hoursPerWeek(), BigDecimal::add);
            }
        }
        Map<String, WeeklyProjectAllocation> existingAllocMap = new HashMap<>();
        for (YearWeek yw : targetWeeks) {
            List<WeeklyProjectAllocation> weekAllocations = loadAllocationPort.loadAllocationsForProjectInWeekRange(
                    targetProject.getId().value(), yw.year(), yw.weekNumber(), yw.weekNumber());
            for (WeeklyProjectAllocation alloc : weekAllocations) {
                existingAllocMap.put(alloc.getEmployeeId() + "_" + yw.year() + "_" + yw.weekNumber(), alloc);
            }
        }

        validateCapacity(desiredHoursByEmployee, employees, targetProject, targetWeeks, existingAllocMap, template.getId());

        int appliedRolesCount = assignments.size();
        int allocatedEmployeesCount = desiredHoursByEmployee.size();
        int unassignedRolesCount = 0;
        List<String> warnings = new ArrayList<>();

        for (ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand item : assignments) {
            ProjectRole role = roleMap.get(item.roleId());
            for (YearWeek yw : targetWeeks) {
                ProjectResourceDemand demand = loadDemandPort
                        .findByProjectIdAndRoleIdAndYearWeek(
                                targetProject.getId(), new ProjectRoleId(item.roleId()), yw)
                        .map(existing -> {
                            existing.updateRequiredHours(item.hoursPerWeek());
                            return existing;
                        })
                        .orElseGet(() -> ProjectResourceDemand.createNew(
                                targetProject.getId(), new ProjectRoleId(item.roleId()), yw, item.hoursPerWeek()));
                saveDemandPort.save(demand);
            }
            if (item.employeeId() == null) {
                unassignedRolesCount++;
                warnings.add("Cần bổ sung nhân sự cho vai trò " + role.getName());
            }
        }

        Set<Long> affectedEmployees = new HashSet<>(desiredHoursByEmployee.keySet());
        for (WeeklyProjectAllocation existing : existingAllocMap.values()) {
            affectedEmployees.add(existing.getEmployeeId());
        }

        for (Long employeeId : affectedEmployees) {
            BigDecimal desiredTemplateHours = desiredHoursByEmployee.getOrDefault(employeeId, BigDecimal.ZERO);

            for (YearWeek yw : targetWeeks) {
                WeeklyProjectAllocation existingAlloc = getExistingAllocation(employeeId, targetProject, yw, existingAllocMap);
                String newVarianceNote = buildVarianceNoteWithTemplateTag(
                        existingAlloc != null ? existingAlloc.getVarianceNote() : null,
                        template.getId(),
                        desiredTemplateHours
                );

                if (existingAlloc != null) {
                    existingAlloc.updateAllocation(desiredTemplateHours, null, currentUserId);
                    existingAlloc.updateVarianceNote(newVarianceNote, currentUserId);
                    saveAllocationPort.save(existingAlloc);
                } else if (desiredTemplateHours.compareTo(BigDecimal.ZERO) > 0) {
                    WeeklyProjectAllocation newAlloc = WeeklyProjectAllocation.createNew(
                            employeeId,
                            targetProject.getId().value(),
                            yw,
                            desiredTemplateHours
                    );
                    newAlloc.updateVarianceNote(newVarianceNote, currentUserId);
                    saveAllocationPort.save(newAlloc);
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

    private void validateAssignments(
            List<ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand> assignments,
            Map<Long, ProjectRoleAllocationTemplateItem> templateItems,
            Map<Long, ProjectRole> roleMap) {
        if (assignments.size() != templateItems.size()) {
            throw new InvalidRoleAllocationTemplateException("Danh sách phân bổ phải chứa đúng các vai trò của mẫu");
        }
        Set<Long> seenRoles = new HashSet<>();
        for (ApplyRoleAllocationTemplateCommand.RoleAssignmentItemCommand item : assignments) {
            ProjectRoleAllocationTemplateItem templateItem = templateItems.get(item.roleId());
            if (templateItem == null || !seenRoles.add(item.roleId()) || !roleMap.containsKey(item.roleId())) {
                throw new InvalidRoleAllocationTemplateException("Vai trò phân bổ không hợp lệ: " + item.roleId());
            }
            if (item.hoursPerWeek() == null || item.hoursPerWeek().compareTo(templateItem.getHoursPerWeek()) != 0) {
                throw new InvalidRoleAllocationTemplateException("Số giờ của vai trò " + item.roleId() + " không khớp với mẫu");
            }
        }
    }

    private void validateEmployeeAssignment(User currentUser, Employee employee, ProjectRole role, Long employeeId) {
        if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new InvalidRoleAllocationTemplateException("Nhân viên không tồn tại hoặc không hoạt động: " + employeeId);
        }
        String professionalRole = employee.getProfessionalRole();
        if (professionalRole == null || (!professionalRole.equalsIgnoreCase(role.getCode())
                && !professionalRole.equalsIgnoreCase(role.getName()))) {
            throw new InvalidRoleAllocationTemplateException("Nhân viên " + employeeId + " không phù hợp với vai trò " + role.getName());
        }
        requireOrgUnitInDataScope(currentUser, employee.getOrgUnitId());
    }

    private void validateCapacity(
            Map<Long, BigDecimal> desiredHoursByEmployee,
            Map<Long, Employee> employees,
            Project targetProject,
            List<YearWeek> targetWeeks,
            Map<String, WeeklyProjectAllocation> existingAllocMap,
            Long templateId) {
        for (Map.Entry<Long, BigDecimal> desired : desiredHoursByEmployee.entrySet()) {
            Long employeeId = desired.getKey();
            BigDecimal desiredTemplateHours = desired.getValue();
            Employee employee = employees.get(employeeId);

            for (YearWeek yw : targetWeeks) {
                BigDecimal available = resolveAvailableHours(employee, yw);
                BigDecimal allocatedToOtherProjects = loadAllocationPort.loadAllocationsForEmployee(employeeId, yw).stream()
                        .filter(allocation -> !targetProject.getId().value().equals(allocation.getProjectId()))
                        .map(WeeklyProjectAllocation::getAllocatedHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalProjectedHours = allocatedToOtherProjects.add(desiredTemplateHours);
                if (totalProjectedHours.compareTo(available) > 0) {
                    throw new InvalidRoleAllocationTemplateException(
                            "Nhân viên " + employeeId + " không đủ năng lực trong tuần " + yw.weekNumber());
                }
            }
        }
    }

    private WeeklyProjectAllocation getExistingAllocation(
            Long employeeId,
            Project targetProject,
            YearWeek yw,
            Map<String, WeeklyProjectAllocation> existingAllocMap) {
        String key = employeeId + "_" + yw.year() + "_" + yw.weekNumber();
        if (existingAllocMap.containsKey(key)) {
            return existingAllocMap.get(key);
        }
        Optional<WeeklyProjectAllocation> allocOpt = loadAllocationPort.loadAllocation(
                employeeId, targetProject.getId().value(), yw);
        if (allocOpt.isPresent()) {
            WeeklyProjectAllocation alloc = allocOpt.get();
            existingAllocMap.put(key, alloc);
            return alloc;
        }
        return null;
    }

    private static final Pattern TEMPLATE_TAG_PATTERN = Pattern.compile("\\[ROLE_TEMPLATE:(\\d+):([0-9]+(?:\\.[0-9]+)?)\\]");

    private BigDecimal extractTemplateHours(String varianceNote, Long templateId) {
        if (varianceNote == null || varianceNote.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = TEMPLATE_TAG_PATTERN.matcher(varianceNote);
        while (matcher.find()) {
            Long tid = Long.valueOf(matcher.group(1));
            if (tid.equals(templateId)) {
                return new BigDecimal(matcher.group(2));
            }
        }
        return BigDecimal.ZERO;
    }

    private String buildVarianceNoteWithTemplateTag(String existingNote, Long templateId, BigDecimal templateHours) {
        String cleanNote = removeTemplateTag(existingNote, templateId);
        if (templateHours == null || templateHours.compareTo(BigDecimal.ZERO) <= 0) {
            return cleanNote.isEmpty() ? null : cleanNote;
        }
        String tag = String.format(Locale.ROOT, "[ROLE_TEMPLATE:%d:%.2f]", templateId, templateHours);
        if (cleanNote.isEmpty()) {
            return tag;
        }
        return cleanNote + " " + tag;
    }

    private String removeTemplateTag(String varianceNote, Long templateId) {
        if (varianceNote == null || varianceNote.isEmpty()) {
            return "";
        }
        Matcher matcher = TEMPLATE_TAG_PATTERN.matcher(varianceNote);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            Long tid = Long.valueOf(matcher.group(1));
            if (tid.equals(templateId)) {
                matcher.appendReplacement(sb, "");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private BigDecimal resolveAvailableHours(Employee employee, YearWeek yw) {
        int standardHours = (employee != null && employee.getStandardHoursPerWeek() != null)
                ? employee.getStandardHoursPerWeek()
                : 40;
        Long empId = employee != null ? employee.getIdValue() : null;
        if (empId == null) {
            return BigDecimal.valueOf(standardHours);
        }
        return loadWeeklyAvailabilityPort.findByEmployeeIdAndYearWeek(empId, yw)
                .map(WeeklyAvailability::getNetAvailableHours)
                .orElse(BigDecimal.valueOf(standardHours));
    }

    private User loadCurrentUser(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE));
    }

    private void requireProjectInDataScope(User currentUser, Project project) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadProjectPort.existsInOrgUnitBranch(project.getId().value(), currentUser.getScopeOrgUnitId());
            case SELF -> false;
        };
        if (!allowed) {
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }
    }

    private void requireOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        if (!isOrgUnitInDataScope(currentUser, orgUnitId)) {
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_MANAGE);
        }
    }

    private boolean isOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH -> orgUnitId != null && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
            case SELF -> false;
        };
    }

    private void requireActiveProject(Project project) {
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ProjectInactiveException("Dự án không ở trạng thái hoạt động: " + project.getId().value());
        }
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
