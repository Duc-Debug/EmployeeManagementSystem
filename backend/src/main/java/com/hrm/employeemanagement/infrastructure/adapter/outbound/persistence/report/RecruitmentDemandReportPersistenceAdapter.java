package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.domain.skill.SkillStatus;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.EmployeeSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;

@Component
public class RecruitmentDemandReportPersistenceAdapter implements LoadRecruitmentDemandReportPort {

    private final SpringDataSkillRepository skillRepository;
    private final SpringDataProjectRoleRepository projectRoleRepository;
    private final SpringDataProjectResourceDemandRepository projectResourceDemandRepository;
    private final SpringDataEmployeeSkillRepository employeeSkillRepository;

    public RecruitmentDemandReportPersistenceAdapter(
            SpringDataSkillRepository skillRepository,
            SpringDataProjectRoleRepository projectRoleRepository,
            SpringDataProjectResourceDemandRepository projectResourceDemandRepository,
            SpringDataEmployeeSkillRepository employeeSkillRepository
    ) {
        this.skillRepository = Objects.requireNonNull(skillRepository, "SpringDataSkillRepository must not be null");
        this.projectRoleRepository = Objects.requireNonNull(projectRoleRepository, "SpringDataProjectRoleRepository must not be null");
        this.projectResourceDemandRepository = Objects.requireNonNull(projectResourceDemandRepository, "SpringDataProjectResourceDemandRepository must not be null");
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "SpringDataEmployeeSkillRepository must not be null");
    }

    @Override
    public List<Skill> loadAllActiveSkills() {
        List<SkillJpaEntity> entities = skillRepository.findAll();
        return entities.stream()
                .map(e -> new Skill(
                        e.getId(),
                        e.getCode(),
                        e.getName(),
                        e.getCategory(),
                        e.getDescription(),
                        e.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, BigDecimal> loadProjectDemandHoursGroupedBySkill(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId) {

        List<SkillJpaEntity> skills = skillRepository.findAll();
        List<ProjectRoleJpaEntity> roles = projectRoleRepository.findAll();
        List<ProjectResourceDemandJpaEntity> demands = projectResourceDemandRepository.findAll();

        Map<Long, ProjectRoleJpaEntity> roleMap = roles.stream()
                .collect(Collectors.toMap(ProjectRoleJpaEntity::getId, r -> r, (a, b) -> a));

        Map<Long, BigDecimal> demandMap = new HashMap<>();

        for (ProjectResourceDemandJpaEntity d : demands) {
            // Lọc theo tuần / năm
            if (fromYear != null && d.getYear() < fromYear) continue;
            if (toYear != null && d.getYear() > toYear) continue;
            if (fromYear != null && fromWeek != null && d.getYear().equals(fromYear) && d.getWeekNumber() < fromWeek) continue;
            if (toYear != null && toWeek != null && d.getYear().equals(toYear) && d.getWeekNumber() > toWeek) continue;

            ProjectRoleJpaEntity role = roleMap.get(d.getRoleId());
            if (role == null) continue;

            // Áp khớp project_role với skill trong danh mục
            Long targetSkillId = resolveSkillIdForRole(role, skills);
            if (targetSkillId != null) {
                demandMap.merge(targetSkillId, d.getRequiredHours(), BigDecimal::add);
            }
        }

        return demandMap;
    }

    @Override
    public Map<Long, BigDecimal> loadAvailableCapacityHoursGroupedBySkill(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId) {

        List<EmployeeSkillJpaEntity> approvedSkills = employeeSkillRepository.findAll().stream()
                .filter(es -> es.getStatus() == SkillStatus.APPROVED)
                .toList();

        int numberOfWeeks = calculateNumberOfWeeks(fromYear, fromWeek, toYear, toWeek);

        Map<Long, BigDecimal> capacityMap = new HashMap<>();

        // Với mỗi nhân sự sở hữu kỹ năng đã phê duyệt, giả định 40h/tuần tiêu chuẩn
        for (EmployeeSkillJpaEntity es : approvedSkills) {
            BigDecimal employeeCapacity = BigDecimal.valueOf(40L * numberOfWeeks);
            capacityMap.merge(es.getSkillId(), employeeCapacity, BigDecimal::add);
        }

        return capacityMap;
    }

    private Long resolveSkillIdForRole(ProjectRoleJpaEntity role, List<SkillJpaEntity> skills) {
        String roleCode = role.getCode().toUpperCase();
        String roleName = role.getName().toLowerCase();

        for (SkillJpaEntity s : skills) {
            String sCode = s.getCode().toUpperCase();
            String sName = s.getName().toLowerCase();

            if (roleCode.equals("TEST") && (sCode.contains("TEST") || sName.contains("kiểm thử") || sName.contains("qa"))) {
                return s.getId();
            }
            if (roleCode.equals("DEV") && (sCode.contains("JAVA") || sName.contains("java") || sCode.contains("DEV"))) {
                return s.getId();
            }
            if (roleCode.equals("BA") && (sCode.contains("BA") || sName.contains("nghiệp vụ"))) {
                return s.getId();
            }
            if (roleCode.equals("UIUX") && (sCode.contains("UI") || sName.contains("thiết kế"))) {
                return s.getId();
            }
            if (roleCode.equals("DEVOPS") && (sCode.contains("DOCKER") || sCode.contains("DEVOPS") || sName.contains("devops"))) {
                return s.getId();
            }
            if (sCode.equals(roleCode) || sName.equalsIgnoreCase(roleName)) {
                return s.getId();
            }
        }

        // Fallback: nếu skill trùng code hoặc có skill đầu tiên
        return skills.stream()
                .filter(s -> s.getCode().equalsIgnoreCase(roleCode) || s.getName().toLowerCase().contains(roleName))
                .map(SkillJpaEntity::getId)
                .findFirst()
                .orElse(skills.isEmpty() ? null : skills.get(0).getId());
    }

    private int calculateNumberOfWeeks(Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek) {
        if (fromYear == null || fromWeek == null || toYear == null || toWeek == null) {
            return 1;
        }
        if (fromYear.equals(toYear)) {
            return Math.max(1, toWeek - fromWeek + 1);
        }
        return Math.max(1, (toYear - fromYear) * 52 + (toWeek - fromWeek + 1));
    }
}
