package com.hrm.employeemanagement.application.service.skill;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.skill.EmployeeSkillResult;
import com.hrm.employeemanagement.application.port.inbound.skill.GetMyEmployeeSkillsUseCase;
import com.hrm.employeemanagement.application.port.outbound.skill.EmployeeSkillRepository;
import com.hrm.employeemanagement.application.port.outbound.skill.SkillCatalogRepository;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.skill.EmployeeSkill;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.user.UserId;

public class GetMyEmployeeSkillsService implements GetMyEmployeeSkillsUseCase {

    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillCatalogRepository skillCatalogRepository;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public GetMyEmployeeSkillsService(
            EmployeeSkillRepository employeeSkillRepository,
            SkillCatalogRepository skillCatalogRepository,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "EmployeeSkillRepository must not be null");
        this.skillCatalogRepository = Objects.requireNonNull(skillCatalogRepository, "SkillCatalogRepository must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public List<EmployeeSkillResult> execute() {
        Long currentUserId = authorizationService.require(PermissionCode.EMPLOYEE_SKILL_READ);

        Optional<Employee> empOpt = loadEmployeePort.findByUserId(new UserId(currentUserId));
        if (empOpt.isEmpty()) {
            return List.of();
        }

        Employee employee = empOpt.get();
        List<EmployeeSkill> skills = employeeSkillRepository.findByEmployeeId(employee.getIdValue());
        if (skills.isEmpty()) {
            return List.of();
        }

        // Tải batch toàn bộ danh mục kỹ năng tương ứng trong 1 query duy nhất (Tránh N+1 query)
        List<Long> skillIds = skills.stream()
                .map(EmployeeSkill::getSkillId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Skill> skillMap = skillCatalogRepository.findAllByIdIn(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity(), (existing, replacing) -> existing));

        return skills.stream()
                .map(es -> EmployeeSkillResult.fromDomain(es, skillMap.get(es.getSkillId())))
                .toList();
    }
}
