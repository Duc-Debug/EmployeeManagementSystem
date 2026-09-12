package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadWeeklyAvailabilityPort;
import com.hrm.employeemanagement.application.port.outbound.calendar.LoadWorkingCalendarPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.application.dto.report.RecruitmentCapacityMetrics;
import com.hrm.employeemanagement.application.dto.report.RecruitmentDemandMetrics;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleSkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.projection.ProjectDemandByRoleProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.entity.SkillJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.projection.EmployeeSkillCapacityProjection;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataEmployeeSkillRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.skill.repository.SpringDataSkillRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitmentDemandReportPersistenceAdapter Tests")
class RecruitmentDemandReportPersistenceAdapterTest {

    @Mock
    private SpringDataSkillRepository skillRepository;

    @Mock
    private SpringDataProjectRoleSkillRepository projectRoleSkillRepository;

    @Mock
    private SpringDataProjectResourceDemandRepository projectResourceDemandRepository;

    @Mock
    private SpringDataEmployeeSkillRepository employeeSkillRepository;

    @Mock
    private LoadWeeklyAvailabilityPort loadWeeklyAvailabilityPort;

    @Mock
    private LoadHolidaysPort loadHolidaysPort;

    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;

    @Mock
    private LoadWorkingCalendarPort loadWorkingCalendarPort;

    @Mock
    private LoadOrgUnitPort loadOrgUnitPort;

    private RecruitmentDemandReportPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RecruitmentDemandReportPersistenceAdapter(
                skillRepository,
                projectRoleSkillRepository,
                projectResourceDemandRepository,
                employeeSkillRepository,
                loadWeeklyAvailabilityPort,
                loadHolidaysPort,
                loadApprovedLeavesPort,
                loadAllocationPort,
                loadWorkingCalendarPort,
                loadOrgUnitPort
        );
    }

    @Test
    @DisplayName("HIGH-04 & HIGH-01: Tải danh sách skill active và filter demand theo orgUnitId")
    void testLoadAllActiveSkills_And_DemandFiltering() {
        SkillJpaEntity activeSkill = new SkillJpaEntity(1L, "JAVA", "Java Skill", "Backend", "Desc", LocalDateTime.now());
        activeSkill.setStatus("ACTIVE");
        activeSkill.setGroupId(10L);

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(activeSkill));

        List<Skill> skills = adapter.loadAllActiveSkills();
        assertEquals(1, skills.size());
        assertEquals("JAVA", skills.get(0).getCode());

        ProjectDemandByRoleProjection demand = new ProjectDemandByRoleProjection() {
            @Override public Long getRoleId() { return 100L; }
            @Override public BigDecimal getRequiredHours() { return BigDecimal.valueOf(120); }
        };
        ProjectRoleSkillJpaEntity mapping = org.mockito.Mockito.mock(ProjectRoleSkillJpaEntity.class);
        when(mapping.getRoleId()).thenReturn(100L);
        when(mapping.getSkillId()).thenReturn(1L);
        when(projectResourceDemandRepository.sumDemandsByRoleFilteredByOrgUnitIds(List.of(10L), 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));
        when(projectRoleSkillRepository.findActiveMappingsByRoleIdIn(List.of(100L))).thenReturn(List.of(mapping));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, 10L);

        assertEquals(1, demandMap.size());
        assertEquals(0, BigDecimal.valueOf(120).compareTo(demandMap.get(1L)));

        verify(projectResourceDemandRepository).sumDemandsByRoleFilteredByOrgUnitIds(List.of(10L), 2026, 1, 2026, 4);
    }

    @Test
    @DisplayName("Case 1 — Single skill: Employee 160h capacity with 1 skill")
    void testLoadAvailableCapacity_Case1_SingleSkill() {
        EmployeeSkillCapacityProjection proj1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 4; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(3); }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnitIds(List.of(5L)))
                .thenReturn(List.of(proj1));

        // 4 weeks => 40h * 4 = 160h
        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, 5L);

        assertEquals(1, capacityMap.size());
        assertEquals(0, BigDecimal.valueOf(160.00).compareTo(capacityMap.get(1L)));
    }

    @Test
    @DisplayName("Case 2 — Multiple skills: capacity is split by proficiency without losing hours")
    void testLoadAvailableCapacity_Case2_MultipleSkills_WeightedAttribution() {
        // Employee 101 has 2 skills: Skill 1 (Java, Level 4) and Skill 2 (React, Level 3)
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 4; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(5); }
        };
        EmployeeSkillCapacityProjection emp1Skill2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 3; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(2); }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1, emp1Skill2));

        RecruitmentCapacityMetrics metrics = adapter.loadAvailableCapacityMetrics(2026, 1, 2026, 4, null);
        assertEquals(0, new BigDecimal("91.42857143").compareTo(metrics.capacityHoursBySkill().get(1L)));
        assertEquals(0, new BigDecimal("68.57142857").compareTo(metrics.capacityHoursBySkill().get(2L)));
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.unattributedCapacityHours()));
    }

    @Test
    @DisplayName("Case 3 — Multiple employees, overlapping skills: Capacity accounting <= total net capacity")
    void testLoadAvailableCapacity_Case3_MultipleEmployees_OverlappingSkills() {
        // Employee 101: Java (Level 4) & React (Level 3) -> 4:3 split.
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 4; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(5); }
        };
        EmployeeSkillCapacityProjection emp1Skill2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 3; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(2); }
        };

        // Employee 102: React (Level 5) -> all 160h on React.
        EmployeeSkillCapacityProjection emp2Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 102L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 5; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(6); }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1, emp1Skill2, emp2Skill1));

        RecruitmentCapacityMetrics metrics = adapter.loadAvailableCapacityMetrics(2026, 1, 2026, 4, null);
        Map<Long, BigDecimal> capacityMap = metrics.capacityHoursBySkill();
        assertEquals(0, new BigDecimal("91.42857143").compareTo(capacityMap.getOrDefault(1L, BigDecimal.ZERO)));
        assertEquals(0, new BigDecimal("228.57142857").compareTo(capacityMap.getOrDefault(2L, BigDecimal.ZERO)));
        assertEquals(0, BigDecimal.ZERO.compareTo(metrics.unattributedCapacityHours()));
    }

    @Test
    @DisplayName("Case 4 — Leave + allocation: net capacity is attributed to its approved skill")
    void testLoadAvailableCapacity_Case4_LeaveAndAllocation_MultiSkill() {
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
            @Override public Integer getProficiencyLevel() { return 4; }
            @Override public BigDecimal getYearsOfExperience() { return BigDecimal.valueOf(4); }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1));

        YearWeek yw = YearWeek.of(2026, 1);
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(Map.of(101L, Map.of(yw, BigDecimal.valueOf(8))));

        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(1L, 101L, 10L, yw, BigDecimal.valueOf(16));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(List.of(alloc));

        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 1, null);

        BigDecimal javaCap = capacityMap.getOrDefault(1L, BigDecimal.ZERO);
        assertEquals(0, BigDecimal.valueOf(16.00).compareTo(javaCap));
    }

    @Test
    @DisplayName("HIGH-03: Role không khớp skill nào sẽ trả về unmapped (null)")
    void testUnmappedRole_ReturnsNull() {
        ProjectDemandByRoleProjection demand = new ProjectDemandByRoleProjection() {
            @Override public Long getRoleId() { return 200L; }
            @Override public BigDecimal getRequiredHours() { return BigDecimal.valueOf(100); }
        };
        when(projectResourceDemandRepository.sumDemandsByRoleFiltered(null, 2026, 1, 2026, 4)).thenReturn(List.of(demand));
        when(projectRoleSkillRepository.findActiveMappingsByRoleIdIn(List.of(200L))).thenReturn(List.of());

        RecruitmentDemandMetrics metrics = adapter.loadProjectDemandMetrics(2026, 1, 2026, 4, null);
        assertTrue(metrics.demandHoursBySkill().isEmpty());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(metrics.unmappedDemandHours()));
        assertEquals(1, metrics.unmappedRoleCount());
    }

    @Test
    @DisplayName("Multiple role skills split one role demand instead of multiplying it")
    void testLoadProjectDemand_MultipleMappedSkills_SplitsDemand() {
        ProjectDemandByRoleProjection demand = new ProjectDemandByRoleProjection() {
            @Override public Long getRoleId() { return 300L; }
            @Override public BigDecimal getRequiredHours() { return BigDecimal.valueOf(100); }
        };
        ProjectRoleSkillJpaEntity java = org.mockito.Mockito.mock(ProjectRoleSkillJpaEntity.class);
        ProjectRoleSkillJpaEntity spring = org.mockito.Mockito.mock(ProjectRoleSkillJpaEntity.class);
        when(java.getRoleId()).thenReturn(300L);
        when(java.getSkillId()).thenReturn(1L);
        when(spring.getRoleId()).thenReturn(300L);
        when(spring.getSkillId()).thenReturn(2L);
        when(projectResourceDemandRepository.sumDemandsByRoleFiltered(null, 2026, 1, 2026, 4)).thenReturn(List.of(demand));
        when(projectRoleSkillRepository.findActiveMappingsByRoleIdIn(List.of(300L))).thenReturn(List.of(java, spring));

        RecruitmentDemandMetrics metrics = adapter.loadProjectDemandMetrics(2026, 1, 2026, 4, null);

        assertEquals(0, BigDecimal.valueOf(50).compareTo(metrics.demandHoursBySkill().get(1L)));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(metrics.demandHoursBySkill().get(2L)));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(
                metrics.demandHoursBySkill().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }
}
