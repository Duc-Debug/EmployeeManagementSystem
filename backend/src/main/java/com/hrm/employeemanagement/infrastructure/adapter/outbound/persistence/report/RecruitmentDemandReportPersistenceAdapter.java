package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;
    private final LoadHolidaysPort loadHolidaysPort;
    private final LoadApprovedLeavesPort loadApprovedLeavesPort;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final LoadWorkingCalendarPort loadWorkingCalendarPort;
    private final LoadOrgUnitPort loadOrgUnitPort;

    @Autowired
    public RecruitmentDemandReportPersistenceAdapter(
            SpringDataSkillRepository skillRepository,
            SpringDataProjectRoleRepository projectRoleRepository,
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
        this.projectRoleRepository = Objects.requireNonNull(projectRoleRepository, "SpringDataProjectRoleRepository must not be null");
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

        List<SkillJpaEntity> activeSkills = skillRepository.findAllActiveSkills();

        List<Long> branchIds = resolveScopeBranchOrgUnitIds(orgUnitId);
        List<ProjectResourceDemandJpaEntity> demands;
        if (branchIds != null && !branchIds.isEmpty()) {
            demands = projectResourceDemandRepository.findDemandsFilteredByOrgUnitIds(
                    branchIds, fromYear, fromWeek, toYear, toWeek);
        } else {
            demands = projectResourceDemandRepository.findDemandsFiltered(
                    orgUnitId, fromYear, fromWeek, toYear, toWeek);
        }

        if (demands == null || demands.isEmpty()) {
            return Map.of();
        }

        // [Goal 5 Optimization]: Load only the required project roles by ID instead of findAll()
        List<Long> roleIds = demands.stream()
                .map(ProjectResourceDemandJpaEntity::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<ProjectRoleJpaEntity> roles = roleIds.isEmpty() ? List.of() : projectRoleRepository.findAllById(roleIds);
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

        List<Long> branchIds = resolveScopeBranchOrgUnitIds(orgUnitId);
        List<EmployeeSkillCapacityProjection> projections;
        if (branchIds != null && !branchIds.isEmpty()) {
            projections = employeeSkillRepository.findApprovedCapacityByOrgUnitIds(branchIds);
        } else {
            projections = employeeSkillRepository.findApprovedCapacityByOrgUnit(orgUnitId);
        }

        if (projections == null || projections.isEmpty()) {
            return Map.of();
        }

        List<YearWeek> targetWeeks = buildTargetYearWeeks(fromYear, fromWeek, toYear, toWeek);

        Map<Long, List<EmployeeSkillCapacityProjection>> skillsByEmployee = projections.stream()
                .collect(Collectors.groupingBy(EmployeeSkillCapacityProjection::getEmployeeId));

        List<Long> employeeIds = new ArrayList<>(skillsByEmployee.keySet());

        Map<String, WeeklyAvailability> availabilityMap = Map.of();
        if (loadWeeklyAvailabilityPort != null && !targetWeeks.isEmpty()) {
            List<WeeklyAvailability> avails = loadWeeklyAvailabilityPort.loadAvailabilityForEmployeesAndWeeks(employeeIds, targetWeeks);
            availabilityMap = avails.stream().collect(Collectors.toMap(
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
                    yw -> WeeklyAvailabilityPolicy.calculateHolidayHoursFromHolidays(yw, holidays, workingDays)
            ));
        }

        Map<Long, Map<YearWeek, BigDecimal>> leaveHoursMap = Map.of();
        if (loadApprovedLeavesPort != null && !targetWeeks.isEmpty()) {
            leaveHoursMap = loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(employeeIds, targetWeeks);
        }

        Map<String, BigDecimal> allocationMap = Map.of();
        if (loadAllocationPort != null && !targetWeeks.isEmpty()) {
            List<WeeklyProjectAllocation> allocations = loadAllocationPort.loadAllocationsForEmployeesAndWeeks(employeeIds, targetWeeks);
            allocationMap = allocations.stream().collect(Collectors.groupingBy(
                    a -> makeKey(a.getEmployeeId(), a.getYear(), a.getWeekNumber()),
                    Collectors.reducing(BigDecimal.ZERO, WeeklyProjectAllocation::getAllocatedHours, BigDecimal::add)
            ));
        }

        // Compute net available capacity per employee and attribute to Primary Approved Skill
        Map<Long, BigDecimal> skillCapacityMap = new HashMap<>();

        Comparator<EmployeeSkillCapacityProjection> primarySkillComparator = Comparator
                .<EmployeeSkillCapacityProjection, Integer>comparing(
                        p -> p.getProficiencyLevel() != null ? p.getProficiencyLevel() : 0)
                .thenComparing(
                        p -> p.getYearsOfExperience() != null ? p.getYearsOfExperience() : BigDecimal.ZERO)
                .thenComparing(
                        p -> p.getSkillId() != null ? -p.getSkillId() : Long.MIN_VALUE);

        for (Map.Entry<Long, List<EmployeeSkillCapacityProjection>> entry : skillsByEmployee.entrySet()) {
            Long empId = entry.getKey();
            List<EmployeeSkillCapacityProjection> empSkills = entry.getValue();
            if (empSkills.isEmpty()) continue;

            EmployeeSkillCapacityProjection primarySkill = empSkills.stream()
                    .max(primarySkillComparator)
                    .orElse(null);

            if (primarySkill == null || primarySkill.getSkillId() == null) continue;

            Integer stdHours = empSkills.get(0).getStandardHoursPerWeek();
            int hoursPerWeek = stdHours != null ? stdHours : 40;

            BigDecimal empTotalNetAvailable = BigDecimal.ZERO;

            if (!targetWeeks.isEmpty()) {
                for (YearWeek yw : targetWeeks) {
                    String key = makeKey(empId, yw.year(), yw.weekNumber());

                    WeeklyAvailability savedAvail = availabilityMap.get(key);
                    int standardHours = savedAvail != null ? savedAvail.getStandardHours() : hoursPerWeek;
                    int holidayHours = holidayHoursByWeek.getOrDefault(yw, 0);
                    BigDecimal leaveHours = leaveHoursMap.getOrDefault(empId, Map.of()).getOrDefault(yw, BigDecimal.ZERO);

                    BigDecimal baseAvailable = WeeklyAvailabilityPolicy.calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
                    BigDecimal allocatedHours = allocationMap.getOrDefault(key, BigDecimal.ZERO);

                    BigDecimal netAvailableForWeek = baseAvailable.subtract(allocatedHours);
                    if (netAvailableForWeek.compareTo(BigDecimal.ZERO) < 0) {
                        netAvailableForWeek = BigDecimal.ZERO;
                    }
                    empTotalNetAvailable = empTotalNetAvailable.add(netAvailableForWeek);
                }
            } else {
                int numberOfWeeks = calculateNumberOfWeeks(fromYear, fromWeek, toYear, toWeek);
                empTotalNetAvailable = BigDecimal.valueOf((long) hoursPerWeek * numberOfWeeks);
            }

            if (empTotalNetAvailable.compareTo(BigDecimal.ZERO) > 0) {
                skillCapacityMap.merge(primarySkill.getSkillId(), empTotalNetAvailable, BigDecimal::add);
            }
        }

        return skillCapacityMap;
    }

    private Long resolveSkillIdForRole(ProjectRoleJpaEntity role, List<SkillJpaEntity> skills) {
        if (role == null || skills == null || skills.isEmpty()) {
            return null;
        }

        if (role.getCode() != null) {
            for (SkillJpaEntity s : skills) {
                if (s.getCode() != null && s.getCode().equalsIgnoreCase(role.getCode())) {
                    return s.getId();
                }
            }
        }

        if (role.getName() != null) {
            for (SkillJpaEntity s : skills) {
                if (s.getName() != null && s.getName().equalsIgnoreCase(role.getName())) {
                    return s.getId();
                }
            }
        }

        if (role.getSkillGroupId() != null) {
            List<SkillJpaEntity> groupSkills = skills.stream()
                    .filter(s -> role.getSkillGroupId().equals(s.getGroupId()))
                    .toList();
            if (groupSkills.size() == 1) {
                return groupSkills.get(0).getId();
            }
        }

        return null;
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
