package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.EmployeeSkillCapacityProjection;
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
        List<SkillJpaEntity> entities = skillRepository.findAllActiveSkills();
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

        List<SkillJpaEntity> activeSkills = skillRepository.findAllActiveSkills();
        List<ProjectRoleJpaEntity> roles = projectRoleRepository.findAll();
        List<ProjectResourceDemandJpaEntity> demands = projectResourceDemandRepository.findDemandsFiltered(
                orgUnitId, fromYear, fromWeek, toYear, toWeek);

        Map<Long, ProjectRoleJpaEntity> roleMap = roles.stream()
                .collect(Collectors.toMap(ProjectRoleJpaEntity::getId, r -> r, (a, b) -> a));

        Map<Long, BigDecimal> demandMap = new HashMap<>();

        for (ProjectResourceDemandJpaEntity d : demands) {
            ProjectRoleJpaEntity role = roleMap.get(d.getRoleId());
            if (role == null) continue;

            Long targetSkillId = resolveSkillIdForRole(role, activeSkills);
            if (targetSkillId != null) {
                demandMap.merge(targetSkillId, d.getRequiredHours(), BigDecimal::add);
            }
        }

        return demandMap;
    }

    @Override
    public Map<Long, BigDecimal> loadAvailableCapacityHoursGroupedBySkill(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId) {

        List<EmployeeSkillCapacityProjection> projections = employeeSkillRepository.findApprovedCapacityByOrgUnit(orgUnitId);

        int numberOfWeeks = calculateNumberOfWeeks(fromYear, fromWeek, toYear, toWeek);

        // Nhóm các kỹ năng được duyệt theo từng nhân sự để phân bổ số giờ làm việc chuẩn
        // Tránh tình trạng double-count capacity khi một nhân sự sở hữu nhiều kỹ năng được phê duyệt
        Map<Long, List<EmployeeSkillCapacityProjection>> skillsByEmployee = projections.stream()
                .collect(Collectors.groupingBy(EmployeeSkillCapacityProjection::getEmployeeId));

        Map<Long, BigDecimal> capacityMap = new HashMap<>();

        for (Map.Entry<Long, List<EmployeeSkillCapacityProjection>> entry : skillsByEmployee.entrySet()) {
            List<EmployeeSkillCapacityProjection> empSkills = entry.getValue();
            int approvedSkillsCount = empSkills.size();
            if (approvedSkillsCount == 0) continue;

            Integer stdHours = empSkills.get(0).getStandardHoursPerWeek();
            int hoursPerWeek = stdHours != null ? stdHours : 40;
            BigDecimal totalEmployeeCapacity = BigDecimal.valueOf((long) hoursPerWeek * numberOfWeeks);

            // Phân bổ năng lực tổng của nhân sự đều cho các kỹ năng đã phê duyệt
            BigDecimal capacityPerSkill = totalEmployeeCapacity.divide(
                    BigDecimal.valueOf(approvedSkillsCount), 2, RoundingMode.HALF_UP);

            for (EmployeeSkillCapacityProjection es : empSkills) {
                capacityMap.merge(es.getSkillId(), capacityPerSkill, BigDecimal::add);
            }
        }

        return capacityMap;
    }

    private Long resolveSkillIdForRole(ProjectRoleJpaEntity role, List<SkillJpaEntity> skills) {
        if (role == null || skills == null || skills.isEmpty()) {
            return null;
        }

        // 1. Khớp theo skillGroupId == groupId
        if (role.getSkillGroupId() != null) {
            for (SkillJpaEntity s : skills) {
                if (role.getSkillGroupId().equals(s.getGroupId())) {
                    return s.getId();
                }
            }
        }

        // 2. Khớp theo code chính xác
        if (role.getCode() != null) {
            for (SkillJpaEntity s : skills) {
                if (s.getCode() != null && s.getCode().equalsIgnoreCase(role.getCode())) {
                    return s.getId();
                }
            }
        }

        // 3. Khớp theo name chính xác
        if (role.getName() != null) {
            for (SkillJpaEntity s : skills) {
                if (s.getName() != null && s.getName().equalsIgnoreCase(role.getName())) {
                    return s.getId();
                }
            }
        }

        return null;
    }

    private int calculateNumberOfWeeks(Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek) {
        if (fromYear == null || fromWeek == null || toYear == null || toWeek == null) {
            return 1;
        }
        LocalDate fromDate = LocalDate.now()
                .with(IsoFields.WEEK_BASED_YEAR, fromYear)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, fromWeek)
                .with(DayOfWeek.MONDAY);
        LocalDate toDate = LocalDate.now()
                .with(IsoFields.WEEK_BASED_YEAR, toYear)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, toWeek)
                .with(DayOfWeek.MONDAY);
        return Math.max(1, (int) (ChronoUnit.WEEKS.between(fromDate, toDate) + 1));
    }
}
