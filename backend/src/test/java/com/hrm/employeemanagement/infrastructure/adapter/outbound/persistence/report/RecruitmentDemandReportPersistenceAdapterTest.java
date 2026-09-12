package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadApprovedLeavesPort;
import com.hrm.employeemanagement.application.port.outbound.availability.LoadHolidaysPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.skill.Skill;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectResourceDemandJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectRoleJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectResourceDemandRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRoleRepository;
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
    private SpringDataProjectRoleRepository projectRoleRepository;

    @Mock
    private SpringDataProjectResourceDemandRepository projectResourceDemandRepository;

    @Mock
    private SpringDataEmployeeSkillRepository employeeSkillRepository;

    @Mock
    private LoadApprovedLeavesPort loadApprovedLeavesPort;

    @Mock
    private LoadHolidaysPort loadHolidaysPort;

    @Mock
    private LoadWeeklyProjectAllocationPort loadAllocationPort;

    @InjectMocks
    private RecruitmentDemandReportPersistenceAdapter adapter;

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

        ProjectRoleJpaEntity role = new ProjectRoleJpaEntity(100L, "DEV", "Developer", "Desc", 10L, "ACTIVE");
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(role));

        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(1L, 50L, 100L, 2026, 1, BigDecimal.valueOf(120), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(10L, 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, 10L);

        assertEquals(1, demandMap.size());
        assertEquals(BigDecimal.valueOf(120), demandMap.get(1L));

        verify(projectResourceDemandRepository).findDemandsFiltered(10L, 2026, 1, 2026, 4);
    }

    @Test
    @DisplayName("Case 1 — Single skill: Employee 160h capacity with 1 skill")
    void testLoadAvailableCapacity_Case1_SingleSkill() {
        EmployeeSkillCapacityProjection proj1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(5L))
                .thenReturn(List.of(proj1));

        // 4 weeks => 40h * 4 = 160h
        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, 5L);

        assertEquals(1, capacityMap.size());
        assertEquals(0, BigDecimal.valueOf(160.00).compareTo(capacityMap.get(1L)));
    }

    @Test
    @DisplayName("Case 2 — Multiple skills: Employee 160h, skills Java & React. Total derived capacity <= 160h (No double counting)")
    void testLoadAvailableCapacity_Case2_MultipleSkills_NoDoubleCounting() {
        // Employee 101 has 2 skills: Skill 1 (Java) and Skill 2 (React), net available = 160h
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };
        EmployeeSkillCapacityProjection emp1Skill2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1, emp1Skill2));

        // Mock demand: Java = 160h, React = 160h
        ProjectRoleJpaEntity javaRole = new ProjectRoleJpaEntity(10L, "JAVA_ROLE", "Java Role", "Desc", 1L, "ACTIVE");
        ProjectRoleJpaEntity reactRole = new ProjectRoleJpaEntity(20L, "REACT_ROLE", "React Role", "Desc", 2L, "ACTIVE");
        SkillJpaEntity javaSkill = new SkillJpaEntity(1L, "JAVA", "Java", "Backend", "Desc", LocalDateTime.now());
        SkillJpaEntity reactSkill = new SkillJpaEntity(2L, "REACT", "React", "Frontend", "Desc", LocalDateTime.now());

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(javaSkill, reactSkill));
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(javaRole, reactRole));

        ProjectResourceDemandJpaEntity javaDemand = new ProjectResourceDemandJpaEntity(1L, 100L, 10L, 2026, 1, BigDecimal.valueOf(160), 0L);
        ProjectResourceDemandJpaEntity reactDemand = new ProjectResourceDemandJpaEntity(2L, 100L, 20L, 2026, 1, BigDecimal.valueOf(160), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(null, 2026, 1, 2026, 4))
                .thenReturn(List.of(javaDemand, reactDemand));

        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, null);

        BigDecimal javaCapacity = capacityMap.getOrDefault(1L, BigDecimal.ZERO);
        BigDecimal reactCapacity = capacityMap.getOrDefault(2L, BigDecimal.ZERO);
        BigDecimal totalDerivedCapacity = javaCapacity.add(reactCapacity);

        // Invariant: Total derived capacity from Employee 101 across all skills <= 160h!
        assertTrue(totalDerivedCapacity.compareTo(BigDecimal.valueOf(160.00)) <= 0,
                "Total derived capacity (" + totalDerivedCapacity + ") must not exceed Employee net capacity (160h)");
    }

    @Test
    @DisplayName("Case 3 — Multiple employees, overlapping skills: Capacity accounting <= total net capacity")
    void testLoadAvailableCapacity_Case3_MultipleEmployees_OverlappingSkills() {
        // Employee 101: Java & React (160h)
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };
        EmployeeSkillCapacityProjection emp1Skill2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        // Employee 102: Java only (160h)
        EmployeeSkillCapacityProjection emp2Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 102L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1, emp1Skill2, emp2Skill1));

        // Demand: Java = 200h, React = 100h
        ProjectRoleJpaEntity javaRole = new ProjectRoleJpaEntity(10L, "JAVA_ROLE", "Java Role", "Desc", 1L, "ACTIVE");
        ProjectRoleJpaEntity reactRole = new ProjectRoleJpaEntity(20L, "REACT_ROLE", "React Role", "Desc", 2L, "ACTIVE");
        SkillJpaEntity javaSkill = new SkillJpaEntity(1L, "JAVA", "Java", "Backend", "Desc", LocalDateTime.now());
        SkillJpaEntity reactSkill = new SkillJpaEntity(2L, "REACT", "React", "Frontend", "Desc", LocalDateTime.now());

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(javaSkill, reactSkill));
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(javaRole, reactRole));

        ProjectResourceDemandJpaEntity javaDemand = new ProjectResourceDemandJpaEntity(1L, 100L, 10L, 2026, 1, BigDecimal.valueOf(200), 0L);
        ProjectResourceDemandJpaEntity reactDemand = new ProjectResourceDemandJpaEntity(2L, 100L, 20L, 2026, 1, BigDecimal.valueOf(100), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(null, 2026, 1, 2026, 4))
                .thenReturn(List.of(javaDemand, reactDemand));

        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, null);

        BigDecimal javaCap = capacityMap.getOrDefault(1L, BigDecimal.ZERO);
        BigDecimal reactCap = capacityMap.getOrDefault(2L, BigDecimal.ZERO);
        BigDecimal totalDerivedCap = javaCap.add(reactCap);

        // Total employee net capacity = 160h + 160h = 320h
        // Invariant: Total assigned capacity <= 320h!
        assertTrue(totalDerivedCap.compareTo(BigDecimal.valueOf(320.00)) <= 0,
                "Total derived capacity (" + totalDerivedCap + ") must not exceed total net capacity of workforce (320h)");
    }

    @Test
    @DisplayName("Case 4 — Leave + allocation + multi-skill: Net capacity = 120h, total derived <= 120h")
    void testLoadAvailableCapacity_Case4_LeaveAndAllocation_MultiSkill() {
        EmployeeSkillCapacityProjection emp1Skill1 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 1L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };
        EmployeeSkillCapacityProjection emp1Skill2 = new EmployeeSkillCapacityProjection() {
            @Override public Long getSkillId() { return 2L; }
            @Override public Long getEmployeeId() { return 101L; }
            @Override public Integer getStandardHoursPerWeek() { return 40; }
        };

        when(employeeSkillRepository.findApprovedCapacityByOrgUnit(null))
                .thenReturn(List.of(emp1Skill1, emp1Skill2));

        // Mock 1 week leave (16h) and 1 week allocation (24h) in 4-week range -> Net = 160 - 16 - 24 = 120h
        YearWeek yw = YearWeek.of(2026, 1);
        when(loadApprovedLeavesPort.loadApprovedLeaveHoursForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(Map.of(101L, Map.of(yw, BigDecimal.valueOf(16))));

        WeeklyProjectAllocation alloc = new WeeklyProjectAllocation(1L, 101L, 10L, yw, BigDecimal.valueOf(24));
        when(loadAllocationPort.loadAllocationsForEmployeesAndWeeks(anyList(), anyList()))
                .thenReturn(List.of(alloc));

        // Demand: Java = 100h, React = 50h
        ProjectRoleJpaEntity javaRole = new ProjectRoleJpaEntity(10L, "JAVA_ROLE", "Java Role", "Desc", 1L, "ACTIVE");
        ProjectRoleJpaEntity reactRole = new ProjectRoleJpaEntity(20L, "REACT_ROLE", "React Role", "Desc", 2L, "ACTIVE");
        SkillJpaEntity javaSkill = new SkillJpaEntity(1L, "JAVA", "Java", "Backend", "Desc", LocalDateTime.now());
        SkillJpaEntity reactSkill = new SkillJpaEntity(2L, "REACT", "React", "Frontend", "Desc", LocalDateTime.now());

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(javaSkill, reactSkill));
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(javaRole, reactRole));

        ProjectResourceDemandJpaEntity javaDemand = new ProjectResourceDemandJpaEntity(1L, 100L, 10L, 2026, 1, BigDecimal.valueOf(100), 0L);
        ProjectResourceDemandJpaEntity reactDemand = new ProjectResourceDemandJpaEntity(2L, 100L, 20L, 2026, 1, BigDecimal.valueOf(50), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(null, 2026, 1, 2026, 4))
                .thenReturn(List.of(javaDemand, reactDemand));

        Map<Long, BigDecimal> capacityMap = adapter.loadAvailableCapacityHoursGroupedBySkill(2026, 1, 2026, 4, null);

        BigDecimal javaCap = capacityMap.getOrDefault(1L, BigDecimal.ZERO);
        BigDecimal reactCap = capacityMap.getOrDefault(2L, BigDecimal.ZERO);
        BigDecimal totalDerivedCap = javaCap.add(reactCap);

        // Standard 160h - 16h leave - 24h allocation = 120h net capacity
        // Invariant: Total assigned capacity <= 120h!
        assertTrue(totalDerivedCap.compareTo(BigDecimal.valueOf(120.00)) <= 0,
                "Total derived capacity (" + totalDerivedCap + ") must not exceed Employee net capacity (120h)");
    }

    @Test
    @DisplayName("HIGH-03: Role không khớp skill nào sẽ trả về unmapped (null)")
    void testUnmappedRole_ReturnsNull() {
        SkillJpaEntity javaSkill = new SkillJpaEntity(1L, "JAVA", "Java Skill", "Backend", "Desc", LocalDateTime.now());
        javaSkill.setStatus("ACTIVE");
        javaSkill.setGroupId(10L);

        when(skillRepository.findAllActiveSkills()).thenReturn(List.of(javaSkill));

        ProjectRoleJpaEntity unknownRole = new ProjectRoleJpaEntity(200L, "UNKNOWN", "Unknown Role", "Desc", 99L, "ACTIVE");
        when(projectRoleRepository.findAllById(anyList())).thenReturn(List.of(unknownRole));

        ProjectResourceDemandJpaEntity demand = new ProjectResourceDemandJpaEntity(2L, 50L, 200L, 2026, 1, BigDecimal.valueOf(100), 0L);
        when(projectResourceDemandRepository.findDemandsFiltered(null, 2026, 1, 2026, 4))
                .thenReturn(List.of(demand));

        Map<Long, BigDecimal> demandMap = adapter.loadProjectDemandHoursGroupedBySkill(2026, 1, 2026, 4, null);

        assertEquals(0, demandMap.size());
    }
}
