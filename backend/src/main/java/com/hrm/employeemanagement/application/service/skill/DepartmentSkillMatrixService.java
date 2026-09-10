package com.hrm.employeemanagement.application.service.skill;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixCellResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixRowResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSkillHeaderResult;
import com.hrm.employeemanagement.application.dto.skill.DepartmentSkillMatrixResult.SkillMatrixSummaryResult;
import com.hrm.employeemanagement.application.port.inbound.skill.GetDepartmentSkillMatrixUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class DepartmentSkillMatrixService implements GetDepartmentSkillMatrixUseCase {

    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SkillCatalogRepository skillCatalogRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;

    public DepartmentSkillMatrixService(
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            SkillCatalogRepository skillCatalogRepository,
            EmployeeSkillRepository employeeSkillRepository,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService
    ) {
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.skillCatalogRepository = Objects.requireNonNull(skillCatalogRepository, "SkillCatalogRepository must not be null");
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "EmployeeSkillRepository must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public DepartmentSkillMatrixResult execute(Long orgUnitId) {
        if (orgUnitId == null) {
            throw new IllegalArgumentException("ID đơn vị/bộ phận không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ);

        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại"));

       
        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))
                .orElseThrow(() -> new OrgUnitNotFoundException("Không tìm thấy đơn vị/bộ phận với ID: " + orgUnitId));

        
        requireOrgUnitInScope(currentUser, orgUnitId);

  
        List<Employee> employees = loadEmployeePort.findActiveByOrgUnitId(orgUnitId);

      
        List<Skill> skills = skillCatalogRepository.findAll();

       
        List<EmployeeSkill> approvedSkills;
        if (employees.isEmpty()) {
            approvedSkills = List.of();
        } else {
            List<Long> employeeIds = employees.stream()
                    .map(Employee::getIdValue)
                    .filter(Objects::nonNull)
                    .toList();
            approvedSkills = employeeSkillRepository.findByStatusAndEmployeeIdIn(SkillStatus.APPROVED, employeeIds);
        }

        // Gom nhóm kỹ năng theo nhân viên: Map<EmployeeId, Map<SkillId, EmployeeSkill>>
        Map<Long, Map<Long, EmployeeSkill>> employeeSkillsMap = new HashMap<>();
        Map<Long, Set<Long>> skillEmployeeIdsMap = new HashMap<>();

        for (EmployeeSkill es : approvedSkills) {
            employeeSkillsMap
                    .computeIfAbsent(es.getEmployeeId(), k -> new HashMap<>())
                    .put(es.getSkillId(), es);

            skillEmployeeIdsMap
                    .computeIfAbsent(es.getSkillId(), k -> new HashSet<>())
                    .add(es.getEmployeeId());
        }

        // Xây dựng danh sách Header kỹ năng (TC-01, TC-02: Đánh dấu rủi ro phụ thuộc 1 người)
        List<SkillMatrixSkillHeaderResult> skillHeaders = skills.stream()
                .map(skill -> {
                    int count = skillEmployeeIdsMap.getOrDefault(skill.getId(), Collections.emptySet()).size();
                    boolean singlePersonRisk = (count == 1);
                    return new SkillMatrixSkillHeaderResult(
                            skill.getId(),
                            skill.getCode(),
                            skill.getName(),
                            skill.getCategory(),
                            count,
                            singlePersonRisk
                    );
                })
                .toList();

        // . Xây dựng danh sách Hàng nhân sự (Rows)
        List<SkillMatrixRowResult> rows = employees.stream()
                .map(emp -> {
                    Map<Long, EmployeeSkill> empSkills = employeeSkillsMap.getOrDefault(emp.getIdValue(), Collections.emptyMap());
                    Map<Long, SkillMatrixCellResult> cells = new HashMap<>();

                    for (Map.Entry<Long, EmployeeSkill> entry : empSkills.entrySet()) {
                        EmployeeSkill es = entry.getValue();
                        cells.put(entry.getKey(), new SkillMatrixCellResult(
                                es.getSkillId(),
                                es.getProficiencyLevelValue(),
                                es.getYearsOfExperience(),
                                es.getReviewNotes()
                        ));
                    }

                    return new SkillMatrixRowResult(
                            emp.getIdValue(),
                            emp.getEmployeeCode(),
                            emp.getFullName(),
                            emp.getProfessionalRole(),
                            cells
                    );
                })
                .toList();

        //  Xây dựng Thống kê tổng kết (Summary)
        int singlePersonRiskCount = (int) skillHeaders.stream().filter(SkillMatrixSkillHeaderResult::singlePersonRisk).count();
        int unstaffedCount = (int) skillHeaders.stream().filter(h -> h.employeeCount() == 0).count();
        SkillMatrixSummaryResult summary = new SkillMatrixSummaryResult(
                employees.size(),
                skillHeaders.size(),
                singlePersonRiskCount,
                unstaffedCount
        );

        Long orgUnitIdValue = orgUnit.getId() != null ? orgUnit.getId().getValue() : orgUnitId;
        return new DepartmentSkillMatrixResult(
                orgUnitIdValue,
                orgUnit.getUnitCode(),
                orgUnit.getUnitName(),
                skillHeaders,
                rows,
                summary
        );
    }

    private void requireOrgUnitInScope(User currentUser, Long orgUnitId) {
        boolean inScope = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(orgUnitId, currentUser.getScopeOrgUnitId());
            case SELF -> false;
        };

        if (!inScope) {
            throw new PermissionDeniedException(PermissionCode.EMPLOYEE_SKILL_READ);
        }
    }
}