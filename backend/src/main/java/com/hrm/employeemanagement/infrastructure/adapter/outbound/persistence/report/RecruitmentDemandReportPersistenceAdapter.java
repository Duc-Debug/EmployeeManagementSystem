package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.report.RecruitmentCapacityMetrics;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandMetrics;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.report.LoadRecruitmentDemandReportPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.Holiday;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailability;
import com.hrm.employeemanagement.domain.availability.WeeklyAvailabilityPolicy;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.report.EmployeeCapacityAttributionPolicy;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.projection.ProjectDemandByRoleProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.EmployeeSkillCapacityProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;

@Component
public class RecruitmentDemandReportPersistenceAdapter implements LoadRecruitmentDemandReportPort {

    private final SpringDataSkillRepository skillRepository;
    private final SpringDataProjectRoleSkillRepository projectRoleSkillRepository;
    private final SpringDataProjectResourceDemandRepository projectResourceDemandRepository;
    private final SpringDataEmployeeSkillRepository employeeSkillRepository;
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    @Autowired
    public RecruitmentDemandReportPersistenceAdapter(
            SpringDataSkillRepository skillRepository,
            SpringDataProjectRoleSkillRepository projectRoleSkillRepository,
            SpringDataProjectResourceDemandRepository projectResourceDemandRepository,
            SpringDataEmployeeSkillRepository employeeSkillRepository,
            LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort,
            LoadHolidaysPort loadHolidaysPort,
            LoadApprovedLeavesPort loadApprovedLeavesPort,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            LoadWorkingCalendarPort loadWorkingCalendarPort,
            LoadOrgUnitPort loadOrgUnitPort
    ) {
        this.skillRepository = Objects.requireNonNull(skillRepository, "SpringDataSkillRepository must not be null");
        this.projectRoleSkillRepository = Objects.requireNonNull(projectRoleSkillRepository, "SpringDataProjectRoleSkillRepository must not be null");
        this.projectResourceDemandRepository = Objects.requireNonNull(projectResourceDemandRepository, "SpringDataProjectResourceDemandRepository must not be null");
        this.employeeSkillRepository = Objects.requireNonNull(employeeSkillRepository, "SpringDataEmployeeSkillRepository must not be null");
        this.loadWeeklyAvailabilityPort = Objects.requireNonNull(loadWeeklyAvailabilityPort, "LoadWeeklyAvailabilityPort must not be null");
        this.loadHolidaysPort = Objects.requireNonNull(loadHolidaysPort, "LoadHolidaysPort must not be null");
        this.loadApprovedLeavesPort = Objects.requireNonNull(loadApprovedLeavesPort, "LoadApprovedLeavesPort must not be null");
        this.loadAllocationPort = Objects.requireNonNull(loadAllocationPort, "LoadWeeklyProjectAllocationPort must not be null");
        this.loadWorkingCalendarPort = Objects.requireNonNull(loadWorkingCalendarPort, "LoadWorkingCalendarPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
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
        return loadProjectDemandMetrics(fromYear, fromWeek, toYear, toWeek, orgUnitId).demandHoursBySkill();
    }

    @Override
    public RecruitmentDemandMetrics loadProjectDemandMetrics(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId) {
        List<Long> branchIds = resolveScopeBranchOrgUnitIds(orgUnitId);
        List<ProjectDemandByRoleProjection> demands;
        if (branchIds != null && !branchIds.isEmpty()) {
            demands = projectResourceDemandRepository.sumDemandsByRoleFilteredByOrgUnitIds(
                    branchIds, fromYear, fromWeek, toYear, toWeek);
        } else {
            demands = projectResourceDemandRepository.sumDemandsByRoleFiltered(
                    orgUnitId, fromYear, fromWeek, toYear, toWeek);
        }

        if (demands == null || demands.isEmpty()) {
            return new RecruitmentDemandMetrics(Map.of(), BigDecimal.ZERO, 0);
        }

        List<Long> roleIds = demands.stream()
                .map(ProjectDemandByRoleProjection::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<Long>> skillIdsByRole = projectRoleSkillRepository.findActiveMappingsByRoleIdIn(roleIds).stream()
                .collect(Collectors.groupingBy(ProjectRoleSkillJpaEntity::getRoleId,
                        Collectors.mapping(ProjectRoleSkillJpaEntity::getSkillId, Collectors.toList())));

        Map<Long, BigDecimal> demandMap = new HashMap<>();
        BigDecimal unmappedHours = BigDecimal.ZERO;
        int unmappedRoles = 0;
        for (ProjectDemandByRoleProjection d : demands) {
            List<Long> skillIds = skillIdsByRole.get(d.getRoleId());
            if (skillIds == null || skillIds.isEmpty()) {
                unmappedHours = unmappedHours.add(d.getRequiredHours());
                unmappedRoles++;
                continue;
            }
            // Mappings form one combined role requirement. Split its hours to
            // prevent a multi-skill role from inflating total demand.
            EmployeeCapacityAttributionPolicy.allocateHours(
                    d.getRequiredHours(), EmployeeCapacityAttributionPolicy.equalWeights(skillIds))
                    .forEach((skillId, hours) -> demandMap.merge(skillId, hours, BigDecimal::add));
        }
        return new RecruitmentDemandMetrics(Map.copyOf(demandMap), unmappedHours, unmappedRoles);
    }

    @Override
    public Map<Long, BigDecimal> loadAvailableCapacityHoursGroupedBySkill(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId) {
        return loadAvailableCapacityMetrics(fromYear, fromWeek, toYear, toWeek, orgUnitId).capacityHoursBySkill();
    }

    @Override
    public RecruitmentCapacityMetrics loadAvailableCapacityMetrics(
            Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek, Long orgUnitId) {

        List<Long> branchIds = resolveScopeBranchOrgUnitIds(orgUnitId);
        List<EmployeeSkillCapacityProjection> projections;
        if (branchIds != null && !branchIds.isEmpty()) {
            projections = employeeSkillRepository.findApprovedCapacityByOrgUnitIds(branchIds);
        } else {
            projections = employeeSkillRepository.findApprovedCapacityByOrgUnit(orgUnitId);
        }

        if (projections == null || projections.isEmpty()) {
            return new RecruitmentCapacityMetrics(Map.of(), BigDecimal.ZERO);
        }

        List<YearWeek> targetWeeks = buildTargetYearWeeks(fromYear, fromWeek, toYear, toWeek);

        Map<Long, List<EmployeeSkillCapacityProjection>> skillsByEmployee = projections.stream()
                .collect(Collectors.groupingBy(EmployeeSkillCapacityProjection::getEmployeeId));

        List<Long> employeeIds = new ArrayList<>(skillsByEmployee.keySet());

        Map<String, WeeklyAvailability> availabilityMap = Map.of();
        if (loadWeeklyAvailabilityPort != null && !targetWeeks.isEmpty()) {
            List<WeeklyAvailability> avails = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
            availabilityMap = (avails == null ? List.<WeeklyAvailability>of() : avails).stream().collect(Collectors.toMap(
                    a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                    a -> a,
                    (existing, replacing) -> existing
            ));
        }

        Map<YearWeek, Integer> holidayHoursByWeek = Map.of();
        if (loadHolidaysPort != null && !targetWeeks.isEmpty()) {
            LocalDate minStart = targetWeeks.get(0).getStartDate();
            LocalDate maxEnd = targetWeeks.get(targetWeeks.size() - 1).getEndDate();
            List<Holiday> holidays = loadHolidaysPort.getHolidaysBetween(minStart, maxEnd);
            Set<DayOfWeek> workingDays = resolveWorkingDays();
            holidayHoursByWeek = targetWeeks.stream().collect(Collectors.toMap(
                    yw -> yw,
                    yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays == null ? List.of() : holidays, workingDays)
            ));
        }

        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = Map.of();
        if (loadApprovedLeavesPort != null && !targetWeeks.isEmpty()) {
            leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);
            if (leaveHoursMap == null) leaveHoursMap = Map.of();
        }

        Map<String, BigDecimal> allocationMap = Map.of();
        if (loadAllocationPort != null && !targetWeeks.isEmpty()) {
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
            allocationMap = (allocations == null ? List.<WeeklyProjectAllocation>of() : allocations).stream().collect(Collectors.groupingBy(
                    a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                    Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
            ));
        }

        // Compute real remaining capacity before any skill attribution.
        Map<Long, BigDecimal> skillCapacityMap = new HashMap<>();
        // Every projection represents an approved skill.  With the weighted
        // allocation policy, all positive employee capacity is attributable.
        BigDecimal unattributedCapacity = BigDecimal.ZERO;

        for (Map.Entry<Long, List<EmployeeSkillCapacityProjection>> entry : skillsByEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<EmployeeSkillCapacityProjection> empSkills = entry.getValue();
            if (empSkills.isEmpty()) continue;

            Map<Long, Integer> proficiencyBySkillId = empSkills.stream()
                    .filter(skill -> skill.getSkillId() != null)
                    .collect(Collectors.toMap(
                            EmployeeSkillCapacityProjection::getSkillId,
                            skill -> skill.getProficiencyLevel() == null ? 1 : skill.getProficiencyLevel(),
                            Math::max));
            if (proficiencyBySkillId.isEmpty()) continue;

            int hoursPerWeek = resolveEmployeeStandardHours(empSkills);

            BigDecimal empTotalNetAvailable = BigDecimal.ZERO;

            for (YearWeek yw : targetWeeks) {
                String key = makeKey(empId, yw.year(), yw.weekNumber());
                WeeklyAvailability savedAvail = availabilityMap.get(key);
                int standardHours = savedAvail != null ? savedAvail.getStandardHours() : hoursPerWeek;
                int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                BigDecimal leaveHours = leaveHoursMap.getOrDefault(empId, Map.of()).getOrDefault(yw, BigDecimal.ZERO);
                BigDecimal baseAvailable = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
                BigDecimal netAvailableForWeek = baseAvailable.subtract(allocationMap.getOrDefault(key, BigDecimal.ZERO));
                empTotalNetAvailable = empTotalNetAvailable.add(netAvailableForWeek.max(BigDecimal.ZERO));
            }

            if (empTotalNetAvailable.compareTo(BigDecimal.ZERO) > 0) {
                EmployeeCapacityAttributionPolicy.allocateHours(empTotalNetAvailable, proficiencyBySkillId)
                        .forEach((skillId, hours) -> skillCapacityMap.merge(skillId, hours, BigDecimal::add));
            }
        }
        return new RecruitmentCapacityMetrics(Map.copyOf(skillCapacityMap), unattributedCapacity);
    }

    private List<YearWeek> buildTargetYearWeeks(Integer fromYear, Integer fromWeek, Integer toYear, Integer toWeek) {
        if (fromYear == null || fromWeek == null || toYear == null || toWeek == null) {
            return List.of();
        }
        List<YearWeek> list = new ArrayList<>();
        YearWeek start = YearWeek.of(fromYear, fromWeek);
        YearWeek end = YearWeek.of(toYear, toWeek);
        LocalDate monday = start.getStartDate();
        LocalDate endMonday = end.getStartDate();

        while (!monday.isAfter(endMonday)) {
            int year = monday.get(IsoFields.WEEK_BASED_YEAR);
            int week = monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            list.add(YearWeek.of(year, week));
            monday = monday.plusWeeks(1);
        }
        return list;
    }

    private List<Long> resolveScopeBranchOrgUnitIds(Long effectiveOrgUnitId) {
        if (effectiveOrgUnitId != null && loadOrgUnitPort != null) {
            Optional<OrgUnit> unitOpt = loadOrgUnitPort.findById(new OrgUnitId(effectiveOrgUnitId));
            if (unitOpt.isPresent()) {
                List<OrgUnit> subTree = loadOrgUnitPort.findSubTree(unitOpt.get().getTreePath());
                return subTree.stream().map(u -> u.getId().getValue()).toList();
            }
            return List.of(effectiveOrgUnitId);
        }
        return null;
    }

    private int resolveEmployeeStandardHours(List<EmployeeSkillCapacityProjection> skills) {
        List<Integer> employeeHours = skills.stream().map(EmployeeSkillCapacityProjection::getStandardHoursPerWeek)
                .filter(Objects::nonNull).distinct().toList();
        if (employeeHours.size() > 1) {
            throw new IllegalStateException("Employee standard hours must be employee-level and consistent");
        }
        // The persistence query uses the established employee default of 40 when the legacy column is null.
        return employeeHours.isEmpty() ? 40 : employeeHours.get(0);
    }

    private String makeKey(Long employeeId, int year, int weekNumber) {
        return employeeId + "_" + year + "_" + weekNumber;
    }

    private Set<DayOfWeek> resolveWorkingDays() {
        if (loadWorkingCalendarPort != null) {
            com.hrm.employeemanagement.domain.calendar.CompanyWorkingCalendar calendar = loadWorkingCalendarPort.loadCompanyCalendar();
            if (calendar != null && calendar.getWorkingDays() != null && !calendar.getWorkingDays().isEmpty()) {
                return calendar.getWorkingDays();
            }
        }
        return Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
        );
    }
}
